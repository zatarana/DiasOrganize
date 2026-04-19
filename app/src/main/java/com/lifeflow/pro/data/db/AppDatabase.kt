package com.lifeflow.pro.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.lifeflow.pro.data.db.dao.AccountDao
import com.lifeflow.pro.data.db.dao.BudgetDao
import com.lifeflow.pro.data.db.dao.CategoryDao
import com.lifeflow.pro.data.db.dao.DebtDao
import com.lifeflow.pro.data.db.dao.DebtInstallmentDao
import com.lifeflow.pro.data.db.dao.GoalDao
import com.lifeflow.pro.data.db.dao.TaskDao
import com.lifeflow.pro.data.db.dao.TransactionDao
import com.lifeflow.pro.data.db.dao.TransferDao
import com.lifeflow.pro.data.db.entities.AccountEntity
import com.lifeflow.pro.data.db.entities.BudgetEntity
import com.lifeflow.pro.data.db.entities.CategoryEntity
import com.lifeflow.pro.data.db.entities.DebtEntity
import com.lifeflow.pro.data.db.entities.DebtInstallmentEntity
import com.lifeflow.pro.data.db.entities.GoalEntity
import com.lifeflow.pro.data.db.entities.TaskEntity
import com.lifeflow.pro.data.db.entities.TransactionEntity
import com.lifeflow.pro.data.db.entities.TransferEntity

@Database(
    entities = [
        AccountEntity::class,
        CategoryEntity::class,
        TaskEntity::class,
        TransactionEntity::class,
        TransferEntity::class,
        DebtEntity::class,
        DebtInstallmentEntity::class,
        BudgetEntity::class,
        GoalEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun taskDao(): TaskDao
    abstract fun transactionDao(): TransactionDao
    abstract fun transferDao(): TransferDao
    abstract fun debtDao(): DebtDao
    abstract fun debtInstallmentDao(): DebtInstallmentDao
    abstract fun budgetDao(): BudgetDao
    abstract fun goalDao(): GoalDao
}
