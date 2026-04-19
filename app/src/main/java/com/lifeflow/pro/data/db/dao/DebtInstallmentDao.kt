package com.lifeflow.pro.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.lifeflow.pro.data.db.entities.DebtInstallmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DebtInstallmentDao {
    @Query("SELECT * FROM debt_installments ORDER BY dueDate ASC, installmentNumber ASC")
    fun observeAll(): Flow<List<DebtInstallmentEntity>>

    @Query("SELECT * FROM debt_installments WHERE debtId = :debtId ORDER BY installmentNumber ASC")
    fun observeByDebtId(debtId: Long): Flow<List<DebtInstallmentEntity>>

    @Query("SELECT * FROM debt_installments WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): DebtInstallmentEntity?

    @Query("SELECT * FROM debt_installments WHERE debtId = :debtId ORDER BY installmentNumber ASC")
    suspend fun getByDebtIdOnce(debtId: Long): List<DebtInstallmentEntity>


    @Query("SELECT * FROM debt_installments ORDER BY dueDate ASC, installmentNumber ASC")
    suspend fun getAllSnapshot(): List<DebtInstallmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: DebtInstallmentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<DebtInstallmentEntity>)

    @Update
    suspend fun update(entity: DebtInstallmentEntity)

    @Delete
    suspend fun delete(entity: DebtInstallmentEntity)
}
