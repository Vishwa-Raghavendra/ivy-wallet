package com.ivy.wallet.core.ui

import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import com.ivy.frp.view.navigation.Navigation
import com.ivy.wallet.core.model.GroupedTransaction
import com.ivy.wallet.core.ui.items.TransactionCardNew
import com.ivy.wallet.core.utils.totalExpense
import com.ivy.wallet.core.utils.totalIncome
import com.ivy.wallet.domain.data.TransactionType
import com.ivy.wallet.domain.data.core.Account
import com.ivy.wallet.domain.data.core.Category
import com.ivy.wallet.ui.EditTransaction
import com.ivy.wallet.ui.ItemStatistic
import java.util.*

fun LazyListScope.listTransactions(
    currencyCode: String,
    transactions: List<GroupedTransaction>,
    nav: Navigation,
    onDateCollapse: (GroupedTransaction.TransactionDate) -> Unit = {}
) {
    historySectionNew(
        historyTransactions = transactions,
        currencyCode = currencyCode,
        onDateCollapse = onDateCollapse,
        nav = nav
    )
}


fun LazyListScope.historySectionNew(
    historyTransactions: List<GroupedTransaction>,
    currencyCode: String,
    nav: Navigation,
    onDateCollapse: (GroupedTransaction.TransactionDate) -> Unit = {}
) {
    items(
        items = historyTransactions,
        key = {
            when (it) {
                is GroupedTransaction.TransactionDate -> it.date.toString()
                is GroupedTransaction.ActualTransaction -> it.transactionNew.id.toString()
            }
        }
    ) {
        when (it) {
            is GroupedTransaction.TransactionDate ->
                TransactionNewDateDivider(
                    it,
                    currencyCode = currencyCode,
                    onDateCollapse = { onDateCollapse(it) }
                )
            is GroupedTransaction.ActualTransaction -> {
                if (!it.hide) {
                    TransactionCardNew(
                        transaction = it.transactionNew,
                        showPlannedPaymentsData = false,
                        onTransactionClick = {
                            nav.navigateToEditTransactionScreen(
                                it.transactionNew.id,
                                it.transactionNew.type
                            )
                        },
                        onAccountClick = { acc ->
                            nav.navigateToAccountScreen(acc)
                        },
                        onCategoryClick = { cat ->
                            nav.navigateToCategoryScreen(cat)
                        }
                    )
                }
            }
        }
    }
}


private fun Navigation.navigateToEditTransactionScreen(id: UUID, type: TransactionType) {
    this.navigateTo(
        EditTransaction(
            initialTransactionId = id,
            type = type
        )
    )
}

private fun Navigation.navigateToAccountScreen(account: Account?) {
    account ?: return
    this.navigateTo(
        ItemStatistic(
            accountId = account.id,
            categoryId = null
        )
    )
}

private fun Navigation.navigateToCategoryScreen(category: Category?) {
    category ?: return
    this.navigateTo(
        ItemStatistic(
            accountId = null,
            categoryId = category.id
        )
    )
}


@Composable
private fun LazyItemScope.TransactionNewDateDivider(
    date: GroupedTransaction.TransactionDate,
    currencyCode: String,
    onDateCollapse: () -> Unit = {}
) {
    DateDividerNew(
        date = date.date,
        baseCurrency = currencyCode,
        income = date.stats.totalIncome(),
        expenses = date.stats.totalExpense(),
        onDateClick = onDateCollapse,
        isCollapsed = date.collapsed
    )
}
