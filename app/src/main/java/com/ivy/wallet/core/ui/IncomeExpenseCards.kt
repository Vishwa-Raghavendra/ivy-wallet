package com.ivy.wallet.core.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ivy.design.l0_system.UI
import com.ivy.design.l0_system.style
import com.ivy.wallet.R
import com.ivy.wallet.domain.data.IvyCurrency
import com.ivy.wallet.stringRes
import com.ivy.wallet.ui.theme.*
import com.ivy.wallet.ui.theme.components.IvyButton
import com.ivy.wallet.utils.drawColoredShadow
import com.ivy.wallet.utils.format

@ExperimentalFoundationApi
@Composable
fun IncomeExpenseCardsNew(
    currencyCode: String,

    income: Double,
    expense: Double,
    incomeTransactionCount: Int = 0,
    expenseTransactionCount: Int = 0,

    itemColor: Color = UI.colors.pure,
    incomeHeaderCardClicked: () -> Unit = {},
    expenseHeaderCardClicked: () -> Unit = {},
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(16.dp))

        HeaderCard(
            title = stringRes(R.string.income_uppercase),
            currencyCode = currencyCode,
            amount = income,
            transactionCount = incomeTransactionCount,
            addButtonText = null,
//            addButtonText = if (hasAddButtons) stringResource(R.string.add_income) else null,
            isIncome = true,

            itemColor = itemColor,
            onHeaderCardClicked = { incomeHeaderCardClicked() }
        ) {
            //onAddTransaction(TransactionType.INCOME)
        }

        Spacer(Modifier.width(12.dp))

        HeaderCard(
            title = stringRes(R.string.expenses_uppercase),
            currencyCode = currencyCode,
            amount = expense,
            transactionCount = expenseTransactionCount,
            addButtonText = null,
            //addButtonText = if (hasAddButtons) stringResource(R.string.add_expense) else null,
            isIncome = false,

            itemColor = itemColor,
            onHeaderCardClicked = { expenseHeaderCardClicked() }
        ) {
            //onAddTransaction(TransactionType.EXPENSE)
        }

        Spacer(Modifier.width(16.dp))
    }
}


@Composable
private fun RowScope.HeaderCard(
    title: String,
    currencyCode: String,
    amount: Double,
    transactionCount: Int,

    isIncome: Boolean,
    addButtonText: String?,

    itemColor: Color,

    onHeaderCardClicked: () -> Unit = {},
    onAddClick: () -> Unit
) {
    val backgroundColor = remember(itemColor) {
        if (isDarkColor(itemColor))
            MediumBlack.copy(alpha = 0.9f)
        else
            MediumWhite.copy(alpha = 0.9f)
    }

    val contrastColor = remember(backgroundColor) {
        findContrastTextColor(backgroundColor)
    }

    Column(
        modifier = Modifier
            .weight(1f)
            .drawColoredShadow(
                color = backgroundColor,
                alpha = 0.1f
            )
            .background(backgroundColor, UI.shapes.r2)
            .clickable { onHeaderCardClicked() },
    ) {

        Spacer(Modifier.height(24.dp))

        HeaderTitle(title = title, contrastColor = contrastColor)

        Spacer(Modifier.height(12.dp))

        HeaderAmount(amount = amount, currencyCode, contrastColor)

        HeaderCurrencyText(currencyCode, contrastColor)

        Spacer(Modifier.height(12.dp))

        HeaderTransactionCount(transactionCount, contrastColor)

        HeaderTransactionsText(contrastColor)

        Spacer(Modifier.height(24.dp))

        ExtraButtons(
            isIncome = isIncome,
            addButtonText = addButtonText,
            contrastColor = contrastColor,
            onAddClick = onAddClick
        )
    }
}

@Composable
private fun HeaderTitle(title: String, contrastColor: Color) {
    Text(
        modifier = Modifier.padding(horizontal = 24.dp),
        text = title,
        style = UI.typo.c.style(
            color = contrastColor,
            fontWeight = FontWeight.ExtraBold
        )
    )
}

@Composable
private fun HeaderAmount(amount: Double, currencyCode: String, contrastColor: Color) {
    val formattedAmountWithCurrency = remember(amount, currencyCode) {
        amount.format(currencyCode)
    }

    Text(
        modifier = Modifier.padding(horizontal = 24.dp),
        text = formattedAmountWithCurrency,
        style = UI.typo.nB1.style(
            color = contrastColor,
            fontWeight = FontWeight.ExtraBold
        )
    )
}

@Composable
private fun HeaderCurrencyText(currencyCode: String, contrastColor: Color) {
    val expandedCurrencyTextFromCode = remember(currencyCode) {
        IvyCurrency.fromCode(currencyCode)?.name ?: ""
    }
    Text(
        modifier = Modifier.padding(horizontal = 24.dp),
        text = expandedCurrencyTextFromCode,
        style = UI.typo.b2.style(
            color = contrastColor,
            fontWeight = FontWeight.Normal
        )
    )
}

@Composable
private fun HeaderTransactionCount(transactionCount: Int, contrastColor: Color) {
    Text(
        modifier = Modifier.padding(horizontal = 24.dp),
        text = transactionCount.toString(),
        style = UI.typo.nB1.style(
            color = contrastColor,
            fontWeight = FontWeight.ExtraBold
        )
    )
}

@Composable
private fun HeaderTransactionsText(contrastColor: Color) {
    Text(
        modifier = Modifier.padding(horizontal = 24.dp),
        text = stringRes(R.string.transactions),
        style = UI.typo.b2.style(
            color = contrastColor,
            fontWeight = FontWeight.Normal
        )
    )
}

@Composable
private fun ColumnScope.ExtraButtons(
    isIncome: Boolean,
    addButtonText: String?,
    contrastColor: Color,
    onAddClick: () -> Unit
) {
    if (addButtonText != null) {
        val addButtonBackground = if (isIncome) Green else contrastColor
        IvyButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .align(Alignment.CenterHorizontally),
            text = addButtonText,
            shadowAlpha = 0.1f,
            backgroundGradient = Gradient.solid(addButtonBackground),
            textStyle = UI.typo.b2.style(
                color = findContrastTextColor(addButtonBackground),
                fontWeight = FontWeight.Bold
            ),
            wrapContentMode = false
        ) {
            onAddClick()
        }

        Spacer(Modifier.height(12.dp))
    }
}