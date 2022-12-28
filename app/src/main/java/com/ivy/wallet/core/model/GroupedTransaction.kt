package com.ivy.wallet.core.model

import java.time.LocalDate

sealed class GroupedTransaction {
    data class TransactionDate(
        val date: LocalDate,
        val stats: Stats,
        val collapsed: Boolean = false
    ) : GroupedTransaction()

    data class ActualTransaction(
        val transactionNew: TransactionNew,
        val hide: Boolean = false
    ) : GroupedTransaction()
}
