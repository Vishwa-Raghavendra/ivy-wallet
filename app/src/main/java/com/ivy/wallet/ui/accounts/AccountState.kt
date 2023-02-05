package com.ivy.wallet.ui.accounts

import com.ivy.wallet.core.utils.UiText

data class AccountState(
    val baseCurrency: String = "",
    val accountsData: List<AccountData> = emptyList(),
    val totalBalanceIncludedAndNonArchived: Double = 0.0,
    val totalBalanceIncludedAndNonArchivedText: UiText = UiText.DynamicString(""),
    val reorderVisible: Boolean = false,
    val includeArchivedAccounts: Boolean = false,
)