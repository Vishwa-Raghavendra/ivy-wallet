package com.ivy.core.domain.api.action

import com.ivy.core.data.TimeRange
import com.ivy.core.data.Transaction
import com.ivy.core.data.calculation.ExchangeRates
import com.ivy.core.domain.action.FlowAction
import com.ivy.core.domain.api.data.Collapsable
import com.ivy.core.domain.calculation.history.*
import com.ivy.core.domain.calculation.history.data.*
import com.ivy.core.domain.data.RawStats
import com.ivy.core.domain.pure.util.flattenLatest
import com.ivy.core.persistence.api.recurring.DueTransactionRead
import com.ivy.core.persistence.api.recurring.RecurringRuleRead
import com.ivy.core.persistence.api.transaction.TransactionRead
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import java.util.*
import javax.inject.Inject

interface MockedSelectedPeriodFlow {
    operator fun invoke(): Flow<TimeRange>
}

interface MockedRatesFlow {
    operator fun invoke(): Flow<ExchangeRates>
}

interface MockedCollapsedFlow {
    operator fun invoke(): Flow<Set<Collapsable>>
}

@OptIn(ExperimentalCoroutinesApi::class)
class PeriodDataFlow @Inject constructor(
    private val selectedPeriodFlow: MockedSelectedPeriodFlow,
    private val recurringRuleRead: RecurringRuleRead,
    private val dueTransactionRead: DueTransactionRead,
    private val transactionRead: TransactionRead,
    private val ratesFlow: MockedRatesFlow,
    private val collapsedFlow: MockedCollapsedFlow,
) : FlowAction<PeriodDataFlow.Input, PeriodData>() {
    sealed interface Input {
        object All : Input
        // TODO: Add by Category, by Account, etc
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun createFlow(input: Input): Flow<PeriodData> =
        selectedPeriodFlow().flatMapLatest(::periodDataFlow)

    private fun periodDataFlow(
        period: TimeRange
    ): Flow<PeriodData> = combine(
        rawDueFlow(period),
        rawActualFlow(period)
    ) { rawDue, (rawHistory, rawStats) ->
        ratesFlow().flatMapLatest { rates ->
            // exchanged
            val (due, history, periodValues) = with(rates) {
                Triple(
                    exchangeDue(rawDue),
                    exchangeHistory(rawHistory),
                    exchangeHistoryRawStats(rawStats)
                )
            }
            collapsedFlow().map { collapsed ->
                PeriodData(
                    periodIncome = periodValues.income,
                    periodExpense = periodValues.expense,
                    transactionList = transactionList(
                        due = due,
                        history = history,
                        collapsed = collapsed
                    )
                )
            }
        }
    }.flattenLatest()

    private fun rawDueFlow(
        period: TimeRange
    ): Flow<SortedMap<RawDueDivider, Sorted<Transaction>>> =
        combine(
            recurringRuleRead.many(RecurringRuleRead.Query.ForPeriod(period)),
            dueTransactionRead.many(DueTransactionRead.Query.ForPeriod(period))
        ) { recurringRules, exceptions ->
            val dueTransactions = generateDue(
                rules = recurringRules,
                dueTransactions = exceptions,
                period = period
            )

            groupDueTransactions(dueTransactions)
        }


    private fun rawActualFlow(
        period: TimeRange
    ): Flow<Pair<SortedMap<RawDateDivider, Sorted<Transaction>>, RawStats>> =
        transactionRead.many(
            TransactionRead.Query.ForPeriod(period)
        ).map { trns ->
            groupByDate(trns) to historyRawStats(trns)
        }
}