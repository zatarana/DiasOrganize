package com.lifeflow.pro.data.db

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseSeeder @Inject constructor(
    private val database: AppDatabase,
) {
    suspend fun seedIfNeeded() {
        if (database.categoryDao().getAll().isEmpty()) {
            database.categoryDao().insertAll(SeedData.defaultCategories())
        }
        if (database.accountDao().getAll().isEmpty()) {
            database.accountDao().insert(SeedData.defaultAccount())
        }
    }
}
