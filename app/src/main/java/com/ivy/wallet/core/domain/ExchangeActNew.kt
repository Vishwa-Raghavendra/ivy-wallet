package com.ivy.wallet.core.domain

import com.ivy.wallet.core.data.repository.ExchangeRepository
import com.ivy.wallet.utils.computationThread
import javax.inject.Inject

class ExchangeActNew @Inject constructor(
    private val exchangeRepository: ExchangeRepository
) {

    suspend fun exchangeAmount(amount: Double, fromCurrency: String, toCurrency: String): Double {
        val fromExchangeRate = exchangeRepository.findByCurrency(fromCurrency)
        val toExchangeRate = exchangeRepository.findByCurrency(toCurrency)

        return computationThread {
            return@computationThread  when{
                fromExchangeRate == null || toExchangeRate == null -> 0.0
                fromExchangeRate.rate == 0.0 || toExchangeRate.rate == 0.0 -> 0.0
                fromCurrency == toCurrency -> amount
                else -> {
                    val effectiveRate = toExchangeRate.rate / fromExchangeRate.rate
                    effectiveRate * amount
                }
            }
        }
    }
}