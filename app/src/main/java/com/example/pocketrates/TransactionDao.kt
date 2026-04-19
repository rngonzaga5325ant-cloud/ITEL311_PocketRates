package com.example.pocketrates

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Query("SELECT * FROM transactions ORDER BY id DESC")
    fun getAllTransactions(): LiveData<List<TransactionEntity>>

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)
}