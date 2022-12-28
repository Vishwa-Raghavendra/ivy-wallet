package com.ivy.wallet.ui.pieChartNew

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.insets.navigationBarsPadding
import com.google.accompanist.insets.statusBarsPadding
import com.ivy.design.l0_system.UI
import com.ivy.design.l0_system.style
import com.ivy.frp.view.navigation.navigation
import com.ivy.wallet.R
import com.ivy.wallet.core.model.TransactionNew
import com.ivy.wallet.core.ui.items.CategoryCardNew
import com.ivy.wallet.core.utils.amtFromPieChartMode
import com.ivy.wallet.core.utils.isCategoryUnspecified
import com.ivy.wallet.core.utils.toOldDomain
import com.ivy.wallet.domain.data.core.Category
import com.ivy.wallet.ui.ItemStatistic
import com.ivy.wallet.ui.PieChartStatisticNew
import com.ivy.wallet.ui.ivyWalletCtx
import com.ivy.wallet.ui.onboarding.model.TimePeriod
import com.ivy.wallet.ui.theme.components.*
import com.ivy.wallet.ui.theme.modal.model.Month
import com.ivy.wallet.ui.theme.pureBlur
import com.ivy.wallet.utils.horizontalSwipeListener
import com.ivy.wallet.utils.onScreenStart
import com.ivy.wallet.utils.springBounce

@ExperimentalFoundationApi
@Composable
fun BoxWithConstraintsScope.PieChartStatisticScreenNew(
    screen: PieChartStatisticNew
) {
    val viewModel: PieChartStatisticViewModelNew = viewModel()
    val state by viewModel.state().collectAsState()

    onScreenStart {
        viewModel.onEvent(PieChartStatisticEventNew.Start(screen))
    }

    UI(
        state = state,
        onEventHandler = viewModel::onEvent
    )
}

@Suppress("unused")
@ExperimentalFoundationApi
@Composable
private fun BoxWithConstraintsScope.UI(
    state: PieChartStatisticStateNew,
    onEventHandler: (PieChartStatisticEventNew) -> Unit = {}
) {
    val nav = navigation()
    val lazyState = rememberLazyListState()
    val expanded by remember {
        derivedStateOf {
            lazyState.firstVisibleItemIndex < 1
        }
    }

    val percentExpanded by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = springBounce()
    )

    val isListScrolledUp by remember(percentExpanded) {
        derivedStateOf { percentExpanded < 1f }
    }

    val totalAmount = state.pieChartStats.amtFromPieChartMode(state.pieChartMode)


    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        state = lazyState
    ) {
        stickyHeader {
            TitleBar(
                currencyCode = state.currencyCode,
                totalAmount = totalAmount,
                showTimeModal = state.showTimeModal,
                showBalanceRowMini = isListScrolledUp,
                onClose = {
                    nav.back()
                }
            )
        }
        item {
            PieChartHeader(
                state.currencyCode,
                totalAmount,
                state.pieChartMode,
                balanceRowAlpha = percentExpanded
            )
        }

        item {
            if (state.showCategoriesUnpackOption) {
                ShowUnpackOption(checked = true, onClick = { })
            }
        }

        item {
            Spacer(Modifier.height(40.dp))

            PieChartNewComposable(
                pieChartList = state.chartDataPoints,
                mode = state.pieChartMode,
                selectedCategory = state.selectedCategory,
                onCategoryClicked = { selectedCategory ->
                    onEventHandler(PieChartStatisticEventNew.OnCategoryClicked(selectedCategory))
                }
            )

            Spacer(Modifier.height(48.dp))
        }

        itemsIndexed(
            items = state.pieChartData,
            key = { _, item -> "${item.category.id}${item.totalAmountWithSubCategory}" }
        ) { index, item ->
            if (index != 0) {
                Spacer(Modifier.height(16.dp))
            }

            CategoryCardNew(
                category = item,
                mode = state.pieChartMode,
                totalAmount = totalAmount,
                currencyCode = state.currencyCode,
                onCategoryExpand = {
                    onEventHandler(PieChartStatisticEventNew.OnCategoryExpand(it))
                }
            ) { cat, trnsList ->
                nav.navigateTo(
                    ItemStatistic(
                        categoryId = cat.id,
                        unspecifiedCategory = cat.isCategoryUnspecified(),
                        accountIdFilterList = state.accountIdFilterList,
                        transactions = trnsList.map(TransactionNew::toOldDomain)
                    )
                )
            }
        }

        item {
            Spacer(
                Modifier
                    .height(120.dp)
            )
        }
    }
}

@Composable
private fun PieChartNewComposable(
    pieChartList: Map<Category, Double>,
    mode: PieChartMode,
    selectedCategory: Category?,
    onCategoryClicked: (Category?) -> Unit
) {
    PieChartNew(
        mode = mode,
        categoryAmounts = pieChartList,
        selectedCategory = selectedCategory,
        onCategoryClicked = onCategoryClicked
    )
}

@Composable
private fun ShowUnpackOption(checked: Boolean, onClick: (Boolean) -> Unit) {
    IvyCheckboxWithText(
        modifier = Modifier
            .padding(top = 12.dp, start = 16.dp),
        text = stringResource(R.string.unpack_all_subcategories),
        checked = checked,
        onCheckedChange = onClick
    )
}

@Composable
private fun PieChartHeader(
    currencyCode: String,
    totalAmount: Double,
    pieChartMode: PieChartMode,
    balanceRowAlpha: Float = 1f
) {
    Spacer(Modifier.height(20.dp))

    PieChartTitle(pieChartMode)

    PieChartActualBalanceRow(currencyCode, totalAmount)
}

@Composable
private fun PieChartTitle(pieChartMode: PieChartMode) {
    Spacer(Modifier.height(20.dp))
    Text(
        modifier = Modifier
            .padding(start = 32.dp)
            .testTag("piechart_title_new"),
        text = when (pieChartMode) {
            PieChartMode.EXPENSE -> stringResource(R.string.expenses)
            PieChartMode.INCOME -> stringResource(R.string.income)
            PieChartMode.BALANCE -> "Balance"
        },
        style = UI.typo.b1.style(
            fontWeight = FontWeight.ExtraBold
        )
    )
}

@Composable
private fun PieChartActualBalanceRow(
    currencyCode: String,
    totalAmount: Double,
    balanceRowAlpha: Float = 1f
) {
    BalanceRow(
        modifier = Modifier
            .padding(start = 32.dp, end = 16.dp)
            .testTag("piechart_total_amount_new")
            .alpha(balanceRowAlpha),
        currency = currencyCode,
        balance = totalAmount,
        currencyUpfront = false,
        currencyFontSize = 30.sp
    )
}


@Composable
private fun TitleBar(
    onClose: () -> Unit,
    showBalanceRowMini: Boolean,
    showTimeModal: Boolean,
    currencyCode: String,
    totalAmount: Double
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(pureBlur())
            .statusBarsPadding()
            .padding(top = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(20.dp))

        CloseButton(onClick = onClose)

        if (showBalanceRowMini) {
            Spacer(Modifier.width(12.dp))
            BalanceRowMiniComposable(currencyCode, totalAmount)
        }

        if (showTimeModal) {
            Spacer(Modifier.weight(1f))
            TimeModal(period = TimePeriod(month = Month.fromMonthValue(2), year = 2022))
            Spacer(Modifier.width(12.dp))
        }

    }
}

@Composable
private fun TimeModal(period: TimePeriod) {
    val startDay = ivyWalletCtx().startDayOfMonth
    val dateText = remember(startDay) {
        period.toDisplayShort(startDay)
    }
    IvyOutlinedButton(
        modifier = Modifier.horizontalSwipeListener(
            sensitivity = 75,
            onSwipeLeft = {
                //
            },
            onSwipeRight = {
                //
            }
        ),
        iconStart = R.drawable.ic_calendar,
        text = dateText,
    ) {
        //
    }
}

@Composable
private fun BalanceRowMiniComposable(
    currencyCode: String,
    totalAmount: Double
) {
    BalanceRowMini(
        modifier = Modifier.alpha(1f),
        currency = currencyCode,
        balance = totalAmount,
    )
}
