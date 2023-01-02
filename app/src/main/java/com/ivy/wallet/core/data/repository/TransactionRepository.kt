package com.ivy.wallet.core.data.repository

import com.ivy.wallet.core.model.TransactionNew
import com.ivy.wallet.core.utils.toTransactionDomain
import com.ivy.wallet.io.persistence.dao.TransactionDao
import com.ivy.wallet.io.persistence.data.TransactionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class TransactionRepository @Inject constructor(
    private val transactionDao: TransactionDao,
    private val accountsRepository: AccountsRepository,
    private val categoriesRepository: CategoriesRepository,
    private val tagsRepository: TagsRepository
) {

    suspend fun getAllTransactions(): List<TransactionNew> {
        return withContext(Dispatchers.IO) {
            transactionDao.findAll().toDomain()
        }
    }

    suspend fun findByDate(startDateInMilli: Long, endDateInMilli: Long) : List<TransactionNew> {
        return withContext(Dispatchers.IO) {
            transactionDao.findByDate(startDateInMilli, endDateInMilli).toDomain()
        }
    }

    private suspend fun List<TransactionEntity>.toDomain(): List<TransactionNew> {
        val allAccounts = accountsRepository
            .getAllAccounts()
            .associateBy { it.id }

        val allCategories = categoriesRepository
            .getAllCategories()
            .associateBy { it.id }

        return this.map {
            val tags = tagsRepository.findTagsByTransactionId(it.id)

            it.toTransactionDomain(
                allAccounts[it.accountId]!!,
                allAccounts[it.toAccountId],
                allCategories[it.categoryId],
                tags = tags
            )
        }
    }
}