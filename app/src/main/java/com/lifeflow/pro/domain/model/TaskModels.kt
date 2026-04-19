package com.lifeflow.pro.domain.model

import com.lifeflow.pro.data.db.entities.CategoryEntity
import com.lifeflow.pro.data.db.entities.TaskEntity
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

object TaskDateFormats {
    val date: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    val dateTime: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
}

object TaskConstants {
    const val STATUS_PENDING = "PENDENTE"
    const val STATUS_COMPLETED = "CONCLUIDA"
    const val STATUS_OVERDUE = "ATRASADA"

    const val PRIORITY_LOW = "BAIXA"
    const val PRIORITY_MEDIUM = "MEDIA"
    const val PRIORITY_HIGH = "ALTA"

    const val RECURRENCE_NONE = "NENHUMA"
    const val RECURRENCE_DAILY = "DIARIA"
    const val RECURRENCE_WEEKLY = "SEMANAL"
    const val RECURRENCE_MONTHLY = "MENSAL"
    const val RECURRENCE_CUSTOM = "PERSONALIZADA"
}

data class TaskFilter(val key: String, val label: String)

data class TaskWithCategory(
    val task: TaskEntity,
    val category: CategoryEntity? = null,
)

data class TaskEditorState(
    val id: Long = 0,
    val title: String = "",
    val description: String = "",
    val categoryId: Long? = null,
    val dueDate: String = "",
    val dueTime: String = "",
    val priority: String = TaskConstants.PRIORITY_MEDIUM,
    val recurrenceType: String = TaskConstants.RECURRENCE_NONE,
    val recurrenceConfig: String = "",
    val linkedTransactionId: Long? = null,
) {
    fun toEntity(now: Long = System.currentTimeMillis()): TaskEntity = TaskEntity(
        id = id,
        title = title.trim(),
        description = description.trim().ifBlank { null },
        categoryId = categoryId,
        status = TaskConstants.STATUS_PENDING,
        dueDate = dueDate.ifBlank { null },
        dueTime = dueTime.ifBlank { null },
        recurrenceType = recurrenceType,
        recurrenceConfig = recurrenceConfig.ifBlank { null },
        priority = priority,
        parentTaskId = null,
        linkedTransactionId = linkedTransactionId,
        createdAt = now,
        completedAt = null,
    )

    companion object {
        fun fromEntity(task: TaskEntity): TaskEditorState = TaskEditorState(
            id = task.id,
            title = task.title,
            description = task.description.orEmpty(),
            categoryId = task.categoryId,
            dueDate = task.dueDate.orEmpty(),
            dueTime = task.dueTime.orEmpty(),
            priority = task.priority,
            recurrenceType = task.recurrenceType,
            recurrenceConfig = task.recurrenceConfig.orEmpty(),
            linkedTransactionId = task.linkedTransactionId,
        )
    }
}

object TaskRecurrenceCalculator {
    fun nextInstance(completedTask: TaskEntity): TaskEntity? {
        if (completedTask.recurrenceType == TaskConstants.RECURRENCE_NONE) return null
        val dueDate = completedTask.dueDate?.let { LocalDate.parse(it, TaskDateFormats.date) } ?: LocalDate.now()
        val nextDate = when (completedTask.recurrenceType) {
            TaskConstants.RECURRENCE_DAILY -> dueDate.plusDays(1)
            TaskConstants.RECURRENCE_WEEKLY -> dueDate.plusWeeks(1)
            TaskConstants.RECURRENCE_MONTHLY -> dueDate.plusMonths(1)
            TaskConstants.RECURRENCE_CUSTOM -> nextCustomDate(dueDate, completedTask.recurrenceConfig)
            else -> null
        } ?: return null

        return completedTask.copy(
            id = 0,
            status = TaskConstants.STATUS_PENDING,
            dueDate = nextDate.format(TaskDateFormats.date),
            parentTaskId = completedTask.id,
            completedAt = null,
            createdAt = System.currentTimeMillis(),
        )
    }

    private fun nextCustomDate(currentDate: LocalDate, recurrenceConfig: String?): LocalDate? {
        if (recurrenceConfig.isNullOrBlank()) return currentDate.plusDays(1)
        return when {
            recurrenceConfig.startsWith("interval:") -> {
                val interval = recurrenceConfig.substringAfter(':').toLongOrNull() ?: 1L
                currentDate.plusDays(interval)
            }
            recurrenceConfig.startsWith("days:") -> {
                val days = recurrenceConfig.substringAfter(':')
                    .split(',')
                    .mapNotNull { raw -> raw.trim().takeIf { it.isNotBlank() }?.uppercase() }
                    .mapNotNull { value -> runCatching { DayOfWeek.valueOf(value) }.getOrNull() }
                    .sortedBy { it.value }
                if (days.isEmpty()) return currentDate.plusDays(1)
                var probe = currentDate.plusDays(1)
                repeat(14) {
                    if (days.contains(probe.dayOfWeek)) return probe
                    probe = probe.plusDays(1)
                }
                probe
            }
            else -> currentDate.plusDays(1)
        }
    }
}

object TaskStreakCalculator {
    fun calculateStreak(tasks: List<TaskEntity>, today: LocalDate = LocalDate.now()): Int {
        val completionDates = tasks.mapNotNull { it.completedAt }
            .map { LocalDateTime.parse(it, TaskDateFormats.dateTime).toLocalDate() }
            .toSet()
        if (completionDates.isEmpty()) return 0

        var probe = today
        if (!completionDates.contains(probe) && !completionDates.contains(today.minusDays(1))) return 0
        if (!completionDates.contains(probe)) probe = today.minusDays(1)

        var streak = 0
        while (completionDates.contains(probe)) {
            streak++
            probe = probe.minusDays(1)
        }
        return streak
    }
}

fun LocalDate.toDisplayBr(): String = format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))

fun String.toDisplayDateBr(): String = runCatching { LocalDate.parse(this, TaskDateFormats.date).toDisplayBr() }.getOrElse { this }

fun formatMonthYear(date: LocalDate): String = YearMonth.from(date).toString()

fun dueDateSummary(dueDate: String?, dueTime: String?): String {
    if (dueDate.isNullOrBlank()) return "Sem prazo"
    val dateText = dueDate.toDisplayDateBr()
    return if (dueTime.isNullOrBlank()) dateText else "$dateText às $dueTime"
}

fun recurrenceSummary(type: String, config: String?): String = when (type) {
    TaskConstants.RECURRENCE_NONE -> "Sem recorrência"
    TaskConstants.RECURRENCE_DAILY -> "Diária"
    TaskConstants.RECURRENCE_WEEKLY -> "Semanal"
    TaskConstants.RECURRENCE_MONTHLY -> "Mensal"
    TaskConstants.RECURRENCE_CUSTOM -> {
        when {
            config.isNullOrBlank() -> "Personalizada"
            config.startsWith("interval:") -> "A cada ${config.substringAfter(':')} dias"
            config.startsWith("days:") -> {
                val locale = Locale("pt", "BR")
                val labels = config.substringAfter(':').split(',').mapNotNull {
                    runCatching { DayOfWeek.valueOf(it.trim().uppercase()) }.getOrNull()
                }.joinToString(", ") { it.getDisplayName(TextStyle.SHORT, locale) }
                "Dias: $labels"
            }
            else -> "Personalizada"
        }
    }
    else -> type
}
