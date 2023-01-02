package com.ivy.wallet.core.domain

import com.ivy.frp.action.FPAction
import com.ivy.wallet.core.model.GroupedTransaction
import com.ivy.wallet.core.model.TransactionNew
import com.ivy.wallet.core.utils.atLocalDateTimeToMilliSeconds
import com.ivy.wallet.domain.data.core.Account
import kotlinx.coroutines.*
import java.time.LocalDate
import javax.inject.Inject

class GroupTransactionsAct @Inject constructor(
    private val calculateStatsNew: CalculateStatsNew
) : FPAction<GroupTransactionsAct.Input, GroupTransactionsAct.Output>() {
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)

    override suspend fun Input.compose(): suspend () -> Output {
        val historyTransactions =
            asyncIo {
                sortedHistoryTransactions().groupTransactions(
                    selectedAccounts,
                    treatTransfersAsIncomeExpense
                )
            }

        return suspend {
            Output(emptyList(), emptyList(), historyTransactions.await())
        }
    }

    private fun Input.sortedHistoryTransactions() =
        this.transactions.filter { it.dateTime != null }.sortedByDescending { it.dateTime }

    private suspend fun List<TransactionNew>.groupTransactions(
        selectedAccounts: List<Account>,
        treatTransfersAsIncomeExpense: Boolean
    ): List<GroupedTransaction> {
        return this
            .groupBy { it.dateTime?.toLocalDate() }
            .filterKeys { it != null }
            .toSortedMap { d1: LocalDate?, d2: LocalDate? ->
                if (d1 == null || d2 == null) return@toSortedMap 0

                return@toSortedMap d2.atStartOfDay().compareTo(d1.atStartOfDay())
            }
            .asIterable()
            .pmap { (date, transactionsForTheDay) ->
                if (date == null) emptyList<GroupedTransaction>()

                val stats = calculateStatsNew(
                    CalculateStatsNew.Input(
                        transactionsForTheDay,
                        selectedAccounts = selectedAccounts,
                        treatTransfersAsIncomeExpense = treatTransfersAsIncomeExpense
                    )
                )

                val transactionDate = GroupedTransaction.TransactionDate(date!!, stats)
                val actualTransaction =
                    transactionsForTheDay.map { GroupedTransaction.ActualTransaction(it) }

                listOf(transactionDate) + actualTransaction
            }
            .flatten()
    }


    data class Input(
        val transactions: List<TransactionNew>,
        val selectedAccounts: List<Account>,
        val treatTransfersAsIncomeExpense: Boolean
    )

    data class Output(
        val upcoming: List<GroupedTransaction>,
        val overdue: List<GroupedTransaction>,
        val history: List<GroupedTransaction>
    )

    suspend fun <A, B> Iterable<A>.pmap(f: suspend (A) -> B): List<B> = with(scope) {
        map { async { f(it) } }.awaitAll()
    }
}