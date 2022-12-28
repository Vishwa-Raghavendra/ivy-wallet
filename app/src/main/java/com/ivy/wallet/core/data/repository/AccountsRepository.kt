package com.ivy.wallet.core.data.repository

import com.ivy.wallet.domain.action.settings.BaseCurrencyAct
import com.ivy.wallet.domain.data.core.Account
import com.ivy.wallet.io.persistence.dao.AccountDao
import com.ivy.wallet.io.persistence.data.AccountEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

class AccountsRepository @Inject constructor(
    private val accountsDao: AccountDao,
    private val baseCurrencyAct: BaseCurrencyAct
) {

    suspend fun getAllAccounts(): List<Account> {
        val baseCurrency = baseCurrencyAct(Unit)
        return withContext(Dispatchers.IO) {
            accountsDao.findAll().map { it.toDomain(baseCurrency) }
        }
    }

    suspend fun findById(id: UUID): Account? {
        val baseCurrency = baseCurrencyAct(Unit)
        return withContext(Dispatchers.IO) {
            accountsDao.findById(id)?.toDomain(baseCurrency)
        }
    }
}