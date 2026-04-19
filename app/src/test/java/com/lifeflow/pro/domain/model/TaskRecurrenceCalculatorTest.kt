package com.lifeflow.pro.domain.model

import com.lifeflow.pro.data.db.entities.TaskEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class TaskRecurrenceCalculatorTest {

    private fun task(recurrenceType: String, dueDate: String, recurrenceConfig: String? = null) = TaskEntity(
        id = 10,
        title = "Pagar conta",
        status = TaskConstants.STATUS_COMPLETED,
        dueDate = dueDate,
        recurrenceType = recurrenceType,
        recurrenceConfig = recurrenceConfig,
        priority = TaskConstants.PRIORITY_MEDIUM,
        createdAt = 0L,
        completedAt = "2026-04-19T10:00:00",
    )

    @Test
    fun `daily recurrence creates next day`() {
        val result = TaskRecurrenceCalculator.nextInstance(task(TaskConstants.RECURRENCE_DAILY, "2026-04-19"))
        assertNotNull(result)
        assertEquals("2026-04-20", result?.dueDate)
    }

    @Test
    fun `weekly recurrence creates next week`() {
        val result = TaskRecurrenceCalculator.nextInstance(task(TaskConstants.RECURRENCE_WEEKLY, "2026-04-19"))
        assertEquals("2026-04-26", result?.dueDate)
    }

    @Test
    fun `monthly recurrence creates next month`() {
        val result = TaskRecurrenceCalculator.nextInstance(task(TaskConstants.RECURRENCE_MONTHLY, "2026-04-19"))
        assertEquals("2026-05-19", result?.dueDate)
    }

    @Test
    fun `custom interval recurrence creates by interval`() {
        val result = TaskRecurrenceCalculator.nextInstance(task(TaskConstants.RECURRENCE_CUSTOM, "2026-04-19", "interval:15"))
        assertEquals("2026-05-04", result?.dueDate)
    }

    @Test
    fun `custom weekdays recurrence jumps to next configured day`() {
        val result = TaskRecurrenceCalculator.nextInstance(task(TaskConstants.RECURRENCE_CUSTOM, "2026-04-19", "days:MONDAY,WEDNESDAY"))
        assertEquals("2026-04-20", result?.dueDate)
    }
}
