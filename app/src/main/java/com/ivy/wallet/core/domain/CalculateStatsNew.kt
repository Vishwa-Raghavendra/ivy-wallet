package com.ivy.wallet.core.domain

import android.util.Log
import com.ivy.frp.action.FPAction
import com.ivy.wallet.core.data.repository.ExchangeRepository
import com.ivy.wallet.core.model.Stats
import com.ivy.wallet.core.model.TransactionNew
import com.ivy.wallet.core.utils.roundTo2Digits
import com.ivy.wallet.domain.action.settings.BaseCurrencyAct
import com.ivy.wallet.domain.data.TransactionType
import com.ivy.wallet.domain.data.core.Account
import java.util.UUID
import javax.inject.Inject

class CalculateStatsNew @Inject constructor(
    private val exchangeActNew: ExchangeActNew,
    private val baseCurrencyAct: BaseCurrencyAct,
    private val exchangeRepository: ExchangeRepository
) : FPAction<CalculateStatsNew.Input, Stats>() {

    override suspend fun Input.compose(): suspend () -> Stats {
        val baseCurrency = toCurrency ?: baseCurrencyAct(Unit)
        val selectedAccountIds = selectedAccounts.map { it.id }.toSet()

        val income = asyncIo { incomeTransactions().sumTransactions(baseCurrency) }
        val expense = asyncIo { expenseTransactions().sumTransactions(baseCurrency) }
        val transfersIncome =
            asyncIo { transferTransactions().sumTransfersIncome(baseCurrency, selectedAccountIds) }
        val transfersExpense =
            asyncIo { transferTransactions().sumTransfersExpense(baseCurrency, selectedAccountIds) }

        Log.d("GGGG","HHH"+transfersExpense.await().first)
        return suspend {
            Stats(
                income = income.await().first,
                expense = expense.await().first,

                incomeTransactionsCount = income.await().second,
                expenseTransactionsCount = expense.await().second,

                transfersIncome = transfersIncome.await().first,
                transfersExpense = transfersExpense.await().first,

                transfersIncomeTransCount = transfersIncome.await().second,
                transfersExpenseTransCount = transfersExpense.await().second,
                treatTransfersAsIncomeExpense = treatTransfersAsIncomeExpense,
                currencyCode = baseCurrency
            )
        }
    }

    private fun Input.incomeTransactions() =
        this.transactions.filter { it.type == TransactionType.INCOME }

    private fun Input.expenseTransactions() =
        this.transactions.filter { it.type == TransactionType.EXPENSE }

    private fun Input.transferTransactions() =
        this.transactions.filter { it.type == TransactionType.TRANSFER }


    private suspend fun List<TransactionNew>.sumTransactions(baseCurrency: String): Pair<Double, Int> {
        val totalIncome = this.sumOf {
            amountInBaseCurrency(it.amount, it.account.currency ?: baseCurrency, baseCurrency)
        }.roundTo2Digits()
        return Pair(totalIncome, this.size)
    }

    private suspend fun List<TransactionNew>.sumTransfersIncome(
        baseCurrency: String,
        selectedAccountIds: Set<UUID>
    ): Pair<Double, Int> {
        var transfersIncomeCount = 0

        val sum = this.sumOf { trns ->
            trns.toAccount?.let { toAccount ->
                if (selectedAccountIds.contains(toAccount.id)) {
                    transfersIncomeCount++
                    amountInBaseCurrency(
                        trns.toAmount,
                        toAccount.currency ?: baseCurrency,
                        baseCurrency
                    )
                } else
                    0.0

            } ?: 0.0
        }.roundTo2Digits()

        return Pair(sum, transfersIncomeCount)
    }

    private suspend fun List<TransactionNew>.sumTransfersExpense(
        baseCurrency: String,
        selectedAccountIds: Set<UUID>
    ): Pair<Double, Int> {
        var transfersExpenseCount = 0

        val sum = this.sumOf { trns ->
            if (selectedAccountIds.contains(trns.account.id)) {
                transfersExpenseCount++
                amountInBaseCurrency(
                    trns.amount,
                    trns.account.currency ?: baseCurrency,
                    baseCurrency
                )
            } else
                0.0
        }.roundTo2Digits()

        return Pair(sum, transfersExpenseCount)
    }

    private suspend fun amountInBaseCurrency(
        amount: Double?,
        fromCurrency: String,
        toCurrency: String
    ): Double {
        return io {
            exchangeActNew.exchangeAmount(amount ?: 0.0, fromCurrency, toCurrency)
        }
    }


    data class Input(
        val transactions: List<TransactionNew>,
        val selectedAccounts: List<Account>,
        val treatTransfersAsIncomeExpense: Boolean,
        val toCurrency: String? = null
    )
}