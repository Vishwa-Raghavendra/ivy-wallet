package com.ivy.wallet.core.utils

import com.ivy.wallet.core.model.Stats
import com.ivy.wallet.ui.pieChartNew.PieChartMode
import kotlin.math.abs

fun Stats.totalIncome() =
    income + if (treatTransfersAsIncomeExpense) transfersIncome else 0.0

fun Stats.totalExpense() =
    expense + if (treatTransfersAsIncomeExpense) transfersExpense else 0.0

fun Stats.totalIncomeTransactionCount() =
    incomeTransactionsCount + if (treatTransfersAsIncomeExpense) transfersIncomeTransCount else 0

fun Stats.totalExpenseTransactionCount() =
    expenseTransactionsCount + if (treatTransfersAsIncomeExpense) transfersExpenseTransCount else 0

fun Stats.balance() =
    this.totalIncome() - this.totalExpense()

fun Stats.amtFromPieChartMode(mode: PieChartMode): Double {
    return when (mode) {
        PieChartMode.INCOME -> this.totalIncome()
        PieChartMode.EXPENSE -> this.totalExpense()
        PieChartMode.BALANCE -> this.balance()
    }
}