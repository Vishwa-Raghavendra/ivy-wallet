package com.ivy.wallet.ui.pieChartNew

import com.ivy.wallet.core.model.TransactionNew
import com.ivy.wallet.domain.data.core.Category

data class PieChartData(
    val category: Category,
    val categoryAmount: Double,

    val totalAmountWithSubCategory: Double,

    val percentOfTotalAmount: Double,
    val percentOfTotalAmountWithSubCategory: Double,

    val percentShareWithInParentCategory: Double,
    val isParentCategory: Boolean,
    val show: Boolean,
    val isSelected: Boolean = false,

    //Used for Expanding / Compressing Subcategories
    val isExpanded : Boolean = false,
    val associatedTransactions : List<TransactionNew> = emptyList()
)
