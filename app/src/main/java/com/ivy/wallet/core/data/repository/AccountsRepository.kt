package com.ivy.wallet.core.data.repository

import com.ivy.wallet.core.utils.pmap
import com.ivy.wallet.domain.action.settings.BaseCurrencyAct
import com.ivy.wallet.domain.data.core.Account
import com.ivy.wallet.io.persistence.dao.AccountDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

class AccountsRepository @Inject constructor(
    private val accountsDao: AccountDao,
    private val baseCurrencyAct: BaseCurrencyAct,
    private val metadataRepository: MetadataRepository
) {

    suspend fun getAllAccounts(): List<Account> {
        val baseCurrency = baseCurrencyAct(Unit)
        return withContext(Dispatchers.IO) {
            accountsDao.findAll().pmap {
                val metadata = async { metadataRepository.findByAssociatedId(it.id) }
                it.toDomain(baseCurrency, metadata.await())
            }
        }
    }

    suspend fun findById(id: UUID): Account? {
        val baseCurrency = baseCurrencyAct(Unit)
        return withContext(Dispatchers.IO) {
            val metadata = async { metadataRepository.findByAssociatedId(id) }

            accountsDao.findById(id)?.toDomain(baseCurrency, metadataProperties = metadata.await())
        }
    }
}