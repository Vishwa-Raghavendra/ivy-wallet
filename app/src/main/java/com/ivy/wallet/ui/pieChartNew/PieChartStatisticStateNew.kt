package com.ivy.wallet.ui.pieChartNew

import com.ivy.wallet.core.model.Stats
import com.ivy.wallet.core.utils.statsDummy
import com.ivy.wallet.domain.data.core.Category
import java.util.*

data class PieChartStatisticStateNew(
    val currencyCode: String = "",
    val pieChartStats: Stats = statsDummy(),
    val showTimeModal: Boolean = true,
    val pieChartMode: PieChartMode = PieChartMode.EXPENSE,
    val showCategoriesUnpackOption: Boolean = false,
    val selectedCategory: Category? = null,
    val pieChartData: List<PieChartData> = emptyList(),
    val chartDataPoints: Map<Category, Double> = emptyMap(),
    val accountIdFilterList : List<UUID> = emptyList()
)
