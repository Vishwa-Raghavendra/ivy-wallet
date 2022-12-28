package com.ivy.wallet.core.data.repository

import com.ivy.wallet.core.utils.emptyExchangeRate
import com.ivy.wallet.domain.data.core.ExchangeRate
import com.ivy.wallet.io.persistence.dao.ExchangeRateDao
import com.ivy.wallet.io.persistence.dao.SettingsDao
import com.ivy.wallet.utils.scopedIOThread
import javax.inject.Inject

class ExchangeRepository @Inject constructor(
    private val exchangeRateDao: ExchangeRateDao,
    private val settingsDao: SettingsDao
) {

    suspend fun findByCurrency(currency: String): ExchangeRate? {
        return scopedIOThread {
            val baseCurrency = settingsDao.findAll().takeIf { it.isNotEmpty() }?.first()?.currency
                ?: return@scopedIOThread null

            return@scopedIOThread exchangeRateDao.findByBaseCurrencyAndCurrency(
                baseCurrency,
                currency
            )?.toDomain()
        }
    }
}