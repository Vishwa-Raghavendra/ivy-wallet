package com.ivy.wallet.core.ui

import android.util.Log
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ivy.design.l0_system.Blue
import com.ivy.design.l0_system.UI
import com.ivy.design.l0_system.style
import com.ivy.design.l1_buildingBlocks.data.background
import com.ivy.wallet.R
import com.ivy.wallet.ui.theme.Green
import com.ivy.wallet.ui.theme.Orange
import com.ivy.wallet.ui.theme.Red
import com.ivy.wallet.utils.format
import com.ivy.wallet.utils.formatLocal
import java.time.LocalDate

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LazyItemScope.DateDividerNew(
    date: LocalDate,
    onDateClick: () -> Unit = {},
    spacerTop: Dp = 24.dp,
    baseCurrency: String,
    income: Double,
    expenses: Double,
    isCollapsed: Boolean = false
) {
    val today = remember(date) {
        LocalDate.now()
    }

    Spacer(Modifier.height(spacerTop))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .animateItemPlacement(animationSpec = tween(300)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width( if(isCollapsed) 8.dp else 24.dp))

        if (isCollapsed) {
            CollapsedDateIndicator()
            Spacer(Modifier.width(8.dp))
        }

        DateTimeDisplay(currentDayDate = today, givenDate = date, onDateClick = onDateClick)

        Spacer(Modifier.weight(1f).clickable { onDateClick() })

        DayBalance(income, expenses, baseCurrency)

        Spacer(Modifier.width(32.dp))
    }

    Spacer(Modifier.height(4.dp))
}

@Composable
fun CollapsedDateIndicator() {
    Box(
        Modifier
            .width(8.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(12.dp, 0.dp, 0.dp, 12.dp))
            .background(Blue)
    ) {
        //........
    }
}

@Composable
private fun DateTimeDisplay(
    currentDayDate: LocalDate,
    givenDate: LocalDate,
    onDateClick: () -> Unit = {},
) {
    Column(
        modifier = Modifier.clickable { onDateClick() }
    ) {

        DateDisplay(currentDayDate = currentDayDate, givenDate = givenDate)

        Spacer(Modifier.height(4.dp))

        DayOfWeek(currentDayDate = currentDayDate, givenDate = givenDate)
    }
}

@Composable
private fun DateDisplay(currentDayDate: LocalDate, givenDate: LocalDate) {
    val formattedDateText = remember(givenDate) {
        givenDate.formatLocal(
            if (currentDayDate.year == givenDate.year) "MMMM dd." else "MMM dd. yyy"
        )
    }
    Text(
        text = formattedDateText,
        style = UI.typo.b1.style(
            fontWeight = FontWeight.ExtraBold
        )
    )
}


@Composable
private fun DayOfWeek(currentDayDate: LocalDate, givenDate: LocalDate) {
    val context = LocalContext.current
    val formattedDayOfWeek = remember(givenDate) {
        when (givenDate) {
            currentDayDate -> {
                context.getString(R.string.today)
            }
            currentDayDate.minusDays(1) -> {
                context.getString(R.string.yesterday)
            }
            currentDayDate.plusDays(1) -> {
                context.getString(R.string.tomorrow)
            }
            else -> {
                givenDate.formatLocal("EEEE")
            }
        }
    }

    Text(
        text = formattedDayOfWeek,
        style = UI.typo.c.style(
            fontWeight = FontWeight.Bold
        )
    )
}

@Composable
private fun DayBalance(income: Double, expenses: Double, baseCurrency: String) {
    /**
     * Balance DisplayModes
     * 0 --> Show Balance
     * 1 --> Show Income
     * 2 --> Show Expenses
     */
    var balanceDisplayMode by remember(income, expenses) {
        mutableStateOf(
            if (income == 0.0)
                2
            else if (expenses == 0.0)
                1
            else
                0
        )
    }

    val data = remember(balanceDisplayMode) {
        when (balanceDisplayMode) {
            //Show Balance
            0 -> {
                val cashFlow = (income - expenses)
                val text = "${cashFlow.format(baseCurrency)} $baseCurrency"
                Pair(text, Orange)
            }

            //Show Income
            1 -> {
                val text = "+${income.format(baseCurrency)} $baseCurrency"
                Pair(text, Green)
            }

            //Show Expense
            else -> {
                val text = "-${expenses.format(baseCurrency)} $baseCurrency"
                Pair(text, Red)
            }
        }
    }

    Text(
        modifier = Modifier.clickable {
            if (income != 0.0 && expenses != 0.0)
                balanceDisplayMode = ((balanceDisplayMode + 1) % 3)
        },
        text = data.first,
        style = UI.typo.nB2.style(
            fontWeight = FontWeight.Bold,
            color = data.second
        )
    )

}