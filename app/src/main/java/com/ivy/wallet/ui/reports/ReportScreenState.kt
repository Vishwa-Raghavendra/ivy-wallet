package com.ivy.wallet.ui.reports

import com.ivy.wallet.core.model.GroupedTransaction
import com.ivy.wallet.core.model.Stats
import com.ivy.wallet.core.model.TransactionNew
import com.ivy.wallet.core.utils.statsDummy
import com.ivy.wallet.domain.data.TransactionHistoryItem
import com.ivy.wallet.domain.data.core.Account
import com.ivy.wallet.domain.data.core.Category
import com.ivy.wallet.domain.data.core.Transaction
import com.ivy.wallet.ui.tags.TagState
import java.util.*

data class ReportScreenState(
    val baseCurrency: String = "",
    val balance: Double = 0.0,
    val income: Double = 0.0,
    val expenses: Double = 0.0,
    val upcomingIncome: Double = 0.0,
    val upcomingExpenses: Double = 0.0,
    val overdueIncome: Double = 0.0,
    val overdueExpenses: Double = 0.0,
    val history: List<TransactionHistoryItem> = emptyList(),
    val upcomingTransactions: List<Transaction> = emptyList(),
    val overdueTransactions: List<Transaction> = emptyList(),
    val categories: List<Category> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val upcomingExpanded: Boolean = false,
    val overdueExpanded: Boolean = false,

    val accountIdFilters: List<UUID> = emptyList(),
    val transactions: List<Transaction> = emptyList(),
    val filterOverlayVisible: Boolean = false,
    val showTransfersAsIncExpCheckbox: Boolean = false,
    val treatTransfersAsIncExp: Boolean = false,


    val stats: Stats = statsDummy(),
    val historyTransactionsNew: List<GroupedTransaction> = emptyList(),
    val upcomingTransactionsNew: List<GroupedTransaction> = emptyList(),
    val overdueTransactionsNew: List<GroupedTransaction> = emptyList(),

    val loading: Boolean = false,
    val filter: ReportFilter? = null,
    val transactionsNew: List<TransactionNew> = emptyList(),
    val tagState: TagState = TagState()
)