package com.lifeflow.pro.di

import android.content.Context
import androidx.room.Room
import com.lifeflow.pro.data.db.AppDatabase
import com.lifeflow.pro.data.db.dao.AccountDao
import com.lifeflow.pro.data.db.dao.BudgetDao
import com.lifeflow.pro.data.db.dao.CategoryDao
import com.lifeflow.pro.data.db.dao.DebtDao
import com.lifeflow.pro.data.db.dao.DebtInstallmentDao
import com.lifeflow.pro.data.db.dao.GoalDao
import com.lifeflow.pro.data.db.dao.TaskDao
import com.lifeflow.pro.data.db.dao.TransactionDao
import com.lifeflow.pro.data.db.dao.TransferDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "lifeflow_pro.db",
        ).build()

    @Provides fun provideAccountDao(database: AppDatabase): AccountDao = database.accountDao()
    @Provides fun provideCategoryDao(database: AppDatabase): CategoryDao = database.categoryDao()
    @Provides fun provideTaskDao(database: AppDatabase): TaskDao = database.taskDao()
    @Provides fun provideTransactionDao(database: AppDatabase): TransactionDao = database.transactionDao()
    @Provides fun provideTransferDao(database: AppDatabase): TransferDao = database.transferDao()
    @Provides fun provideDebtDao(database: AppDatabase): DebtDao = database.debtDao()
    @Provides fun provideDebtInstallmentDao(database: AppDatabase): DebtInstallmentDao = database.debtInstallmentDao()
    @Provides fun provideBudgetDao(database: AppDatabase): BudgetDao = database.budgetDao()
    @Provides fun provideGoalDao(database: AppDatabase): GoalDao = database.goalDao()
}
