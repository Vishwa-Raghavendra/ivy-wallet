package com.ivy.wallet.ui.pieChartNew

import androidx.lifecycle.viewModelScope
import com.ivy.frp.viewmodel.FRPViewModel
import com.ivy.wallet.core.data.repository.AccountsRepository
import com.ivy.wallet.core.domain.PieChartsStatsActNew
import com.ivy.wallet.core.model.Stats
import com.ivy.wallet.core.utils.addOrRemove
import com.ivy.wallet.core.utils.statsDummy
import com.ivy.wallet.domain.action.settings.BaseCurrencyAct
import com.ivy.wallet.domain.data.core.Account
import com.ivy.wallet.domain.data.core.Category
import com.ivy.wallet.ui.PieChartStatisticNew
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class PieChartStatisticViewModelNew @Inject constructor(
    private val baseCurrencyAct: BaseCurrencyAct,
    private val pieChartStatsAct: PieChartsStatsActNew,
    private val accountsRepository: AccountsRepository,
) : FRPViewModel<PieChartStatisticStateNew, Nothing>() {

    override val _state: MutableStateFlow<PieChartStatisticStateNew> = MutableStateFlow(
        PieChartStatisticStateNew()
    )

    override suspend fun handleEvent(event: Nothing): suspend () -> PieChartStatisticStateNew {
        TODO("Not yet implemented")
    }

    private val _totalStats: MutableStateFlow<Stats> = MutableStateFlow(statsDummy())

    private val _expandedCategorySet = mutableSetOf<Category>()

    private val _pieChartDataPoints: MutableStateFlow<Map<PieChartData, List<PieChartData>>> =
        MutableStateFlow(emptyMap())

    private val _parentCategoryMap: MutableStateFlow<Map<UUID, Category>> =
        MutableStateFlow(emptyMap())

    private val _pieChartMode: MutableStateFlow<PieChartMode> =
        MutableStateFlow(PieChartMode.EXPENSE)

    fun onEvent(event: PieChartStatisticEventNew) {
        viewModelScope.launch(Dispatchers.Default) {
            when (event) {
                is PieChartStatisticEventNew.Start -> initialiseDataNew(event.screen)
                is PieChartStatisticEventNew.OnCategoryClicked -> onCategoryClicked(event.category)
                is PieChartStatisticEventNew.OnCategoryExpand -> onCategoryExpand(event.category)
            }
        }
    }

    private suspend fun initialiseDataNew(screen: PieChartStatisticNew) {
        if (screen.pieChartMode != _pieChartMode.value)
            updateState { PieChartStatisticStateNew() }

        val (pieChartDataPoints, totalStats) = pieChartStatsAct(
            PieChartsStatsActNew.Parameters(
                transactions = screen.transactions,
                selectedAccounts = mapIdsToAccounts(screen.accountIdFilterList.toSet()),
                stats = screen.totalStats,
                pieChartMode = screen.pieChartMode
            )
        )

        _pieChartMode.value = screen.pieChartMode
        _totalStats.value = totalStats
        _pieChartDataPoints.value = pieChartDataPoints
        _parentCategoryMap.value = pieChartDataPoints.entries.associate { (key, _) ->
            key.category.id to key.category
        }

        val baseCurrency = baseCurrencyAct(Unit)
        val dataPoints = pieChartDataPoints.flattenToPieChartData()
        val chartDataPoints = dataPoints.toChartDataPoints()

        updateState {
            it.copy(
                pieChartStats = screen.totalStats,
                pieChartMode = screen.pieChartMode,
                showTimeModal = screen.showTimeModal,
                accountIdFilterList = screen.accountIdFilterList,
                currencyCode = baseCurrency,
                pieChartData = dataPoints,
                chartDataPoints = chartDataPoints,
            )
        }
    }

    private suspend fun onCategoryExpand(category: Category) {
        _expandedCategorySet.addOrRemove(category)

        val dataPoints = _pieChartDataPoints.value.flattenToPieChartData()
        val chartDataPoints = dataPoints.toChartDataPoints()

        updateState {
            it.copy(
                pieChartData = dataPoints,
                chartDataPoints = chartDataPoints,
            )
        }
    }

    private suspend fun onCategoryClicked(category: Category?) {
        val selectedCategory = if (stateVal().selectedCategory?.id == category?.id)
            null
        else
            category

        val categoryToSort = if (selectedCategory.isSubCategory())
            selectedCategory?.getParentCategory()
        else
            selectedCategory

        val newData = _pieChartDataPoints.value.toList()
            .sortedByDescending { (x, _) -> x.category == categoryToSort }
            .toMap()
            .flattenToPieChartData(selectedCategory)
            .applySelectionTransformation(selectedCategory)


        updateState {
            it.copy(
                selectedCategory = selectedCategory,
                pieChartData = newData
            )
        }
    }

    /** ----------------------------- Category Extension Functions ------------------------------ */

    private fun Category?.isSubCategory(): Boolean = this?.parentCategoryId != null

    private fun Category?.getParentCategory(): Category? {
        return _parentCategoryMap.value[this?.parentCategoryId]
    }

    /** ----------------------------- PieChartData Extension Functions -------------------------- */

    private fun List<PieChartData>.toChartDataPoints(): Map<Category, Double> {
        return this.associate {
            val amount = if (it.isExpanded) it.categoryAmount else it.totalAmountWithSubCategory

            it.category to amount
        }
    }

    private fun Map<PieChartData, List<PieChartData>>.flattenToPieChartData(
        selectedCategory: Category? = null
    ): List<PieChartData> {
        return this.flatMap { (pc, scList) ->
            val subCategoryList = if (_expandedCategorySet.contains(pc.category))
                scList.sortedByDescending { it.category == selectedCategory }
            else
                emptyList()

            listOf(pc) + subCategoryList
        }.applyExpandTransformation()
    }

    private fun List<PieChartData>.applyExpandTransformation(): List<PieChartData> {
        return this.map {
            it.copy(isExpanded = _expandedCategorySet.contains(it.category))
        }
    }

    private fun List<PieChartData>.applySelectionTransformation(
        selectedCategory: Category?
    ): List<PieChartData> {
        return this.map {
            it.copy(isSelected = it.category.id == selectedCategory?.id)
        }
    }

    /** ------------------------------------- Utility Functions --------------------------------- */

    private suspend fun mapIdsToAccounts(accountIdFilterList: Set<UUID>): List<Account> {
        val allAccounts = accountsRepository.getAllAccounts()

        return allAccounts.filter { a -> accountIdFilterList.contains(a.id) }
    }
}
