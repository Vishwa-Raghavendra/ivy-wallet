package com.ivy.core.action.calculate.account

import com.ivy.core.functions.allTime
import com.ivy.data.account.Account
import com.ivy.frp.action.FPAction
import com.ivy.frp.asParamTo
import com.ivy.frp.then
import javax.inject.Inject

class AccBalanceAct @Inject constructor(
    private val accStatsAct: AccStatsAct
) : FPAction<Account, Double>() {
    override suspend fun Account.compose(): suspend () -> Double = AccStatsAct.Input(
        account = this,
        period = allTime(),
        transfersAsIncomeExpense = false
    ) asParamTo accStatsAct then {
        it.balance
    }
}