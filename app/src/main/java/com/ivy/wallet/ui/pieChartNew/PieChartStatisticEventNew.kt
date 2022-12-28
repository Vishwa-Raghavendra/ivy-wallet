package com.ivy.wallet.ui.pieChartNew

import com.ivy.wallet.domain.data.core.Category
import com.ivy.wallet.ui.PieChartStatisticNew

sealed class PieChartStatisticEventNew {
    data class OnCategoryClicked(val category: Category?) : PieChartStatisticEventNew()
    data class OnCategoryExpand(val category: Category) : PieChartStatisticEventNew()
    data class Start(val screen: PieChartStatisticNew) : PieChartStatisticEventNew()
}
