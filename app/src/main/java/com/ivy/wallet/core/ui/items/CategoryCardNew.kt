package com.ivy.wallet.core.ui.items

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ivy.design.l0_system.IvyColors
import com.ivy.design.l0_system.UI
import com.ivy.design.l0_system.style
import com.ivy.wallet.R
import com.ivy.wallet.ui.pieChartNew.PieChartData
import com.ivy.wallet.core.model.TransactionNew
import com.ivy.wallet.domain.data.core.Category
import com.ivy.wallet.ui.pieChartNew.PieChartMode
import com.ivy.wallet.ui.theme.Orange
import com.ivy.wallet.ui.theme.components.ItemIconM
import com.ivy.wallet.ui.theme.components.ItemIconMDefaultIcon
import com.ivy.wallet.ui.theme.findContrastTextColor
import com.ivy.wallet.ui.theme.toComposeColor
import com.ivy.wallet.ui.theme.wallet.AmountCurrencyB1Row
import com.ivy.wallet.ui.theme.wallet.AmountCurrencyB2Row
import com.ivy.wallet.utils.drawColoredShadow
import com.ivy.wallet.utils.thenIf
import kotlin.math.abs

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LazyItemScope.CategoryCardNew(
    category: PieChartData,
    mode: PieChartMode,
    //Refers to Total Expenses/Income or Balance
    totalAmount: Double,
    currencyCode: String,
    onCategoryExpand: (Category) -> Unit = {},
    //TODO: Improve, remove listOption
    onClick: (Category, List<TransactionNew>) -> Unit
) {
    val uiColors = UI.colors
    val cardState by remember(
        category.isSelected,
        category.category.id,
        category.isExpanded,
        category.totalAmountWithSubCategory
    ) {
        mutableStateOf(
            category.toCategoryCardState(
                uiColors,
                mode,
                totalAmount,
                currencyCode
            )
        )
    }

    Row(
        modifier = Modifier
            .padding(
                start = if (!category.isParentCategory)
                    48.dp
                else
                    16.dp,
                end = 16.dp,
                top = 0.dp,
                bottom = 0.dp
            )
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .thenIf(cardState.isSelected) {
                drawColoredShadow(cardState.backgroundColor)
            }
            .clip(UI.shapes.r3)
            .background(cardState.backgroundColor, UI.shapes.r3)
            .clickable {
                onClick(cardState.data.category, category.associatedTransactions)
            }
            .padding(vertical = 16.dp)
            .animateItemPlacement(animationSpec = tween(300)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(
            Modifier
                .width(20.dp)
        )

        CategoryIcon(cardState)

        Spacer(Modifier.width(16.dp))

        CategoryInfo(cardState)

        PercentInfo(category, cardState, onCategoryExpand = onCategoryExpand)
    }
}

@Composable
private fun RowScope.PercentInfo(
    pieChartData: PieChartData,
    displayState: CategoryCardState,
    onCategoryExpand: (Category) -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .padding(end = 16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PercentText(
            amount = if (pieChartData.isParentCategory && pieChartData.isExpanded)
                pieChartData.percentOfTotalAmount
            else
                pieChartData.percentOfTotalAmountWithSubCategory,
            selectedState = displayState.isSelected,
            contrastColor = displayState.textColor,
            onClick = {

            }
        )

        if (displayState.showExpandButton) {
            Icon(
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = {
                        onCategoryExpand(pieChartData.category)
                    }), //enlarge click area
                painter = painterResource(id = R.drawable.ic_expandarrow),
                contentDescription = "contentDescription",
                tint = displayState.textColor
            )
        } else
            Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun PercentText(
    amount: Double,
    selectedState: Boolean,
    contrastColor: Color,
    onClick: () -> Unit = {},
) {
    Text(
        modifier = Modifier
            .clickable { onClick() }
            .animateContentSize(),
        text = "$amount %",
        style = UI.typo.nB2.style(
            color = if (selectedState) contrastColor else UI.colors.pureInverse,
            fontWeight = FontWeight.Normal
        )
    )
}

@Composable
private fun RowScope.CategoryInfo(cardState: CategoryCardState) {
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight(),
        verticalArrangement = Arrangement.Center
    ) {
        CategoryTitle(tileString = cardState.data.category.name, textColor = cardState.textColor)
        CategoryAmount(cardState, cardState.isExpanded)
    }
}

@Composable
private fun ColumnScope.CategoryAmount(
    cardState: CategoryCardState,
    isExpanded: Boolean,
) {
    AmountCurrencyB1Row(
        amount = cardState.totalAmountWithSubCategory,
        currency = cardState.currencyCode,
        textColor = cardState.textColor,
        amountFontWeight = FontWeight.ExtraBold
    )
    AnimatedVisibility(visible = isExpanded) {
        if (isExpanded) {
            Spacer(Modifier.height(4.dp))
            AmountCurrencyB2Row(
                amount = cardState.categoryAmount,
                currency = cardState.currencyCode,
                textColor = cardState.textColor,
                amountFontWeight = FontWeight.ExtraBold
            )
        }
    }
    if (!cardState.data.isParentCategory || isExpanded) {
        Spacer(Modifier.height(8.dp))
        PercentWithInCategory(percent = cardState.data.percentShareWithInParentCategory, cardState)
    }
}

@Composable
private fun ColumnScope.CategoryTitle(tileString: String?, textColor: Color) {
    Text(
        modifier = Modifier
            .padding(end = 16.dp),
        text = tileString ?: stringResource(R.string.unspecified),
        style = UI.typo.b2.style(
            color = textColor,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Start
        )
    )
}

@Composable
private fun ColumnScope.PercentWithInCategory(percent: Double, cardState: CategoryCardState) {
    Text(
        modifier = Modifier
            .padding(end = 16.dp),
        text = "Occupies $percent % with in category ",
        style = UI.typo.c.style(
            color = if (cardState.isSelected) cardState.textColor else Orange,
            fontWeight = FontWeight.Normal,
            textAlign = TextAlign.Start
        )
    )
}

@Composable
private fun CategoryIcon(displayState: CategoryCardState) {
    ItemIconM(
        modifier = Modifier
            .background(displayState.categoryColor, CircleShape)
            .padding(),
        iconName = displayState.data.category.icon,
        tint = displayState.tintOfCategoryColor,
        iconContentScale = ContentScale.None,
        Default = {
            ItemIconMDefaultIcon(
                modifier = Modifier.background(displayState.categoryColor, CircleShape),
                iconName = displayState.data.category.icon,
                defaultIcon = R.drawable.ic_custom_category_m,
                tint = displayState.tintOfCategoryColor
            )
        }
    )
}


private data class CategoryCardState(
    val data: PieChartData,
    val allCategoriesTotalAmount: Double,

    val categoryColor: Color,
    val backgroundColor: Color,
    val tintOfCategoryColor: Color,
    val textColor: Color,

    val isSelected: Boolean,
    val showExpandButton: Boolean,
    val isExpanded: Boolean,
    val currencyCode: String,

    val categoryAmount: Double,
    val totalAmountWithSubCategory: Double,
)

private fun PieChartData.toCategoryCardState(
    uiColors: IvyColors,
    mode: PieChartMode,
    totalAmount: Double,
    currencyCode: String
): CategoryCardState {
    val categoryColor = category.color.toComposeColor()

    val backgroundColor = if (isSelected) categoryColor else uiColors.medium
    val textColor = findContrastTextColor(backgroundColor = backgroundColor)
    val tintOfCategoryColor = findContrastTextColor(categoryColor)

    return CategoryCardState(
        data = this,
        allCategoriesTotalAmount = totalAmount,

        categoryColor = categoryColor,
        backgroundColor = backgroundColor,
        tintOfCategoryColor = tintOfCategoryColor,
        textColor = textColor,

        isSelected = isSelected,
        showExpandButton = isParentCategory && categoryAmount != totalAmountWithSubCategory,
        isExpanded = isExpanded,
        currencyCode = currencyCode,
        categoryAmount = abs(categoryAmount),
        totalAmountWithSubCategory = abs(totalAmountWithSubCategory)
    )
}