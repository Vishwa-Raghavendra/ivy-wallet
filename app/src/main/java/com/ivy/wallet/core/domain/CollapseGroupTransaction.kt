package com.ivy.wallet.core.domain

import com.ivy.wallet.core.model.GroupedTransaction
import java.time.LocalDate
import javax.inject.Inject

class CollapseGroupTransaction @Inject constructor() {
    private val collapsedDates: MutableSet<LocalDate> = mutableSetOf()

    fun filterCollapsedTransactions(
        date: GroupedTransaction.TransactionDate,
        transactions: List<GroupedTransaction>
    ): List<GroupedTransaction> {
        if (collapsedDates.contains(date.date)) {
            collapsedDates.remove(date.date)
        } else
            collapsedDates.add(date.date)

        return transactions.collapseTransactions()
    }

    fun filterCollapsedTransactions(
        transactions: List<GroupedTransaction>
    ): List<GroupedTransaction> {
        return transactions.collapseTransactions()
    }

    private fun List<GroupedTransaction>.collapseTransactions(): List<GroupedTransaction> {
        return this.map {
            when (it) {
                is GroupedTransaction.TransactionDate -> it.copy(
                    collapsed = collapsedDates.contains(it.date)
                )
                is GroupedTransaction.ActualTransaction -> {
                    it.copy(hide = collapsedDates.contains(it.transactionNew.dateTime?.toLocalDate()))
                }
            }
        }
    }
}