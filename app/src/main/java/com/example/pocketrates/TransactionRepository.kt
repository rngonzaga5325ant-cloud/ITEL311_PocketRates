package com.example.pocketrates

import androidx.lifecycle.LiveData

class TransactionRepository(private val dao: TransactionDao) {

    val allTransactions: LiveData<List<TransactionEntity>> = dao.getAllTransactions()

    suspend fun insert(transaction: TransactionEntity) {
        dao.insertTransaction(transaction)
    }

    suspend fun delete(transaction: TransactionEntity) {
        dao.deleteTransaction(transaction)
    }
}