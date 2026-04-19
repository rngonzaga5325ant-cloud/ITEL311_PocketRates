package com.example.pocketrates

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class TransactionViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TransactionRepository
    val allTransactions: LiveData<List<TransactionEntity>>

    init {
        val dao = AppDatabase.getDatabase(application).transactionDao()
        repository = TransactionRepository(dao)
        allTransactions = repository.allTransactions
    }

    fun insert(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.insert(transaction)
        }
    }

    fun delete(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.delete(transaction)
        }
    }
}