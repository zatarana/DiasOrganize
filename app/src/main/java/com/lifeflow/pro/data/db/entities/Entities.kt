package com.lifeflow.pro.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val icon: String,
    val color: String,
    val initialBalance: Double,
    val createdAt: Long,
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val color: String,
    val type: String,
)

@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["dueDate"]),
        Index(value = ["status"]),
        Index(value = ["categoryId"]),
        Index(value = ["linkedTransactionId"]),
    ],
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String? = null,
    val categoryId: Long? = null,
    val status: String,
    val dueDate: String? = null,
    val dueTime: String? = null,
    val recurrenceType: String,
    val recurrenceConfig: String? = null,
    val priority: String,
    val parentTaskId: Long? = null,
    val linkedTransactionId: Long? = null,
    val createdAt: Long,
    val completedAt: String? = null,
)

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["expectedDate"]),
        Index(value = ["status"]),
        Index(value = ["accountId"]),
        Index(value = ["categoryId"]),
        Index(value = ["recurrenceGroupId"]),
    ],
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val accountId: Long,
    val categoryId: Long,
    val description: String? = null,
    val expectedValue: Double,
    val finalValue: Double? = null,
    val expectedDate: String,
    val paymentDate: String? = null,
    val status: String,
    val recurrenceType: String,
    val recurrenceGroupId: String? = null,
    val economy: Double = 0.0,
    val createdAt: Long,
)

@Entity(
    tableName = "transfers",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["fromAccountId"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["toAccountId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index(value = ["fromAccountId"]), Index(value = ["toAccountId"])],
)
data class TransferEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fromAccountId: Long,
    val toAccountId: Long,
    val value: Double,
    val description: String? = null,
    val date: String,
    val createdAt: Long,
)

@Entity(tableName = "debts")
data class DebtEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val creditor: String,
    val description: String? = null,
    val originalValue: Double,
    val negotiatedValue: Double? = null,
    val originDate: String,
    val status: String,
    val totalEconomy: Double = 0.0,
    val createdAt: Long,
)

@Entity(
    tableName = "debt_installments",
    foreignKeys = [
        ForeignKey(
            entity = DebtEntity::class,
            parentColumns = ["id"],
            childColumns = ["debtId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["transactionId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["dueDate"]),
        Index(value = ["status"]),
        Index(value = ["debtId"]),
        Index(value = ["transactionId"]),
    ],
)
data class DebtInstallmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val debtId: Long,
    val installmentNumber: Int,
    val expectedValue: Double,
    val finalValue: Double? = null,
    val dueDate: String,
    val paymentDate: String? = null,
    val status: String,
    val economy: Double = 0.0,
    val transactionId: Long? = null,
)

@Entity(
    tableName = "budgets",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["categoryId", "monthYear"], unique = true)],
)
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryId: Long,
    val monthYear: String,
    val plannedValue: Double,
    val createdAt: Long,
)

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val icon: String,
    val targetValue: Double,
    val currentValue: Double,
    val targetDate: String? = null,
    val status: String,
    val completedAt: Long? = null,
    val createdAt: Long,
)
