package com.ivy.wallet.ui.statistic.level1

import androidx.compose.ui.graphics.Color
import com.ivy.wallet.domain.data.core.Category
import com.ivy.wallet.domain.data.core.Transaction
import com.ivy.wallet.ui.theme.Gray
import com.ivy.wallet.ui.theme.findContrastTextColor
import com.ivy.wallet.ui.theme.toComposeColor

data class CategoryAmount(
    val category: Category?,
    val amount: Double,
    val associatedTransactions: List<Transaction> = emptyList(),
    val isCategoryUnspecified: Boolean = false,
    val subCategoryState: SubCategoryState = SubCategoryState(),
) {
    fun totalAmount(): Double = amount + subCategoryState.subCategoryTotalAmount
    fun clearSubcategoriesAndGet(): CategoryAmount {
        return this.copy(subCategoryState = SubCategoryState())
    }

    fun getRelevantAmount() = if (subCategoryState.subCategoryListExpanded)
        amount
    else
        totalAmount()

    data class SubCategoryState(
        val subCategoriesList: List<CategoryAmount> = emptyList(),
        val subCategoryTotalAmount: Double = 0.0,
        val subCategoryListExpanded: Boolean = false,
    )
}

data class CategoryAmountDisplayState(
    val category: Category?,
    val totalAmount: Double,
    val categoryColor: Color,
    val selectedState: Boolean,
    val backgroundColor: Color,
    val tintOfCategoryColor: Color,
    val textColor: Color,
    val isSubCategoryListExpanded: Boolean,
    val isSubcategoriesPresent:Boolean,
    val currentCategoryAmount:Double,
    private val relevantAmount: Double
){
    fun getRelevantAmount() = relevantAmount
}


fun CategoryAmount.toDisplayState(
    selectedCategory: SelectedCategory?,
    uiColors: com.ivy.design.l0_system.IvyColors
): CategoryAmountDisplayState =
    with(this) {
        val category = category
        val totalAmount = totalAmount()
        val categoryColor = category?.color?.toComposeColor() ?: Gray
        val selectedState = when (category) {
            selectedCategory?.category -> true
            else -> false
        }
        val backgroundColor = if (selectedState) categoryColor else uiColors.medium
        val textColor = findContrastTextColor(backgroundColor = backgroundColor)
        val tintOfCategoryColor = findContrastTextColor(categoryColor)

        val relevantAmount = if (subCategoryState.subCategoryListExpanded)
            amount
        else
            totalAmount()

        return@with CategoryAmountDisplayState(
            category,
            totalAmount,
            categoryColor,
            selectedState,
            backgroundColor,
            tintOfCategoryColor,
            textColor,
            subCategoryState.subCategoryListExpanded,
            subCategoryState.subCategoriesList.isNotEmpty(),
            amount,
            relevantAmount
        )
    }
