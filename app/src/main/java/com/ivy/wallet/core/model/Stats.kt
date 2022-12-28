package com.ivy.wallet.core.model

data class Stats(
    val income: Double,
    val expense: Double,

    val incomeTransactionsCount: Int,
    val expenseTransactionsCount: Int,

    val transfersIncome: Double,
    val transfersExpense: Double,

    val transfersIncomeTransCount: Int,
    val transfersExpenseTransCount: Int,

    val treatTransfersAsIncomeExpense: Boolean = false,
    val currencyCode: String = ""
)