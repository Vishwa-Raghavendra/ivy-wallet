package com.ivy.wallet.core.domain

import com.ivy.frp.action.FPAction
import com.ivy.wallet.core.data.repository.CategoriesRepository
import com.ivy.wallet.core.domain.PieChartsStatsActNew.*
import com.ivy.wallet.core.model.*
import com.ivy.wallet.core.utils.*
import com.ivy.wallet.domain.data.TransactionType
import com.ivy.wallet.domain.data.core.Account
import com.ivy.wallet.domain.data.core.Category
import com.ivy.wallet.ui.pieChartNew.PieChartData
import com.ivy.wallet.ui.pieChartNew.PieChartMode
import javax.inject.Inject
import kotlin.math.abs

class PieChartsStatsActNew @Inject constructor(
    private val calculateStatsNew: CalculateStatsNew,
    private val categoriesRepository: CategoriesRepository
) : FPAction<Parameters, Output>() {

    override suspend fun Parameters.compose(): suspend () -> Output {
        return suspend { computePieChartDataPoints(this) }
    }

    private suspend fun computePieChartDataPoints(parameters: Parameters): Output {
        val pieChartDataPoints = parameters.transactions
            .groupByCategory()
            .calculateStatsPerCategory(parameters)
            .groupByParentCategory()
            .computePieChartData(parameters)

        return Output(pieChartDataPoints, parameters.stats)
    }

    private fun List<TransactionNew>.groupByCategory(): Map<Category, List<TransactionNew>> {
        return groupBy {
            it.category
                ?: if (it.type == TransactionType.TRANSFER)
                    accountTransfersCategory
                else
                    unSpecifiedCategory
        }
    }

    private suspend fun Map<Category, List<TransactionNew>>.calculateStatsPerCategory(parameters: Parameters): List<CategoryStats> {
        return this.mapValues { (cat, transactions) ->
            asyncIo {
                val stats = calculateStatsNew(
                    CalculateStatsNew.Input(
                        transactions = transactions,
                        selectedAccounts = parameters.selectedAccounts,
                        treatTransfersAsIncomeExpense = parameters.stats.treatTransfersAsIncomeExpense
                    )
                )

                CategoryStats(
                    category = cat,
                    stats = stats,
                    associatedTransactions = transactions
                )
            }
        }.map {
            it.value.await()
        }
    }

    private suspend fun List<CategoryStats>.groupByParentCategory(): Map<CategoryStats, List<CategoryStats>> {
        val categories = this.associateBy { it.category.id }

        return this
            .groupBy {
                it.category.parentCategoryId ?: it.category.id
            }
            .mapKeys { (parentCategoryID, _) ->
                val catStat = categories[parentCategoryID]
                if (catStat != null)
                    catStat
                else {
                    val category = categoriesRepository.findById(parentCategoryID)
                    if (category != null)
                        CategoryStats(
                            category = category,
                            stats = statsDummy(),
                            associatedTransactions = emptyList()
                        )
                    else
                        null
                }
            }.filterKeys {
                it != null
            }
            //Hack to make kotlin compiler force non Null
            .entries.associate { (parentCategory, subCategoryList) ->
                parentCategory!! to subCategoryList
            }
            //Remove ParentCategory from Subcategory List
            .mapValues { (pc, subCatList) ->
                subCatList.filter { it.category.id != pc.category.id }
            }
    }

    private fun Map<CategoryStats, List<CategoryStats>>.computePieChartData(params: Parameters): Map<PieChartData, List<PieChartData>> {
        val pieChartMode = params.pieChartMode

        return this.entries.associate { (pc, sc) ->
            val parentCategory = pc.category
            val parentCategoryAmount = pc.stats.amtFromPieChartMode(pieChartMode)

            val subCategoryTotalAmount = sc.sumOf { it.stats.amtFromPieChartMode(pieChartMode) }

            val totalCategoryAmountWithSubCategory = parentCategoryAmount + subCategoryTotalAmount

            val allCategoriesTotalAmount = params.stats.amtFromPieChartMode(pieChartMode)

            val percentOfTotalAmount =
                (parentCategoryAmount / allCategoriesTotalAmount).percent().roundTo2Digits()
            val percentOfTotalAmountWithSubCategory =
                (totalCategoryAmountWithSubCategory / allCategoriesTotalAmount).percent()
                    .roundTo2Digits()

            val percentShareWithInParentCategory =
                (parentCategoryAmount / totalCategoryAmountWithSubCategory).percent()
                    .roundTo2Digits()

            val parentCategoryData = PieChartData(
                category = parentCategory,
                categoryAmount = parentCategoryAmount,
                totalAmountWithSubCategory = totalCategoryAmountWithSubCategory,
                percentOfTotalAmount = percentOfTotalAmount,
                percentOfTotalAmountWithSubCategory = percentOfTotalAmountWithSubCategory,

                percentShareWithInParentCategory = percentShareWithInParentCategory,
                isParentCategory = true,
                show = true,
                associatedTransactions = pc.associatedTransactions
            )

            val sortedSubCategoryData = sc
                .toSubCategoryData(
                    allCategoriesTotalAmount,
                    totalCategoryAmountWithSubCategory,
                    pieChartMode
                )
                .filter {
                    it.totalAmountWithSubCategory != 0.0
                }
                .sortedByDescending { ca ->
                    if (pieChartMode == PieChartMode.BALANCE) abs(ca.categoryAmount) else ca.categoryAmount
                }

            parentCategoryData to sortedSubCategoryData
        }
            .filterKeys {
                it.totalAmountWithSubCategory != 0.0
            }
            .toList()
            .sortedByDescending { (x, _) -> abs(x.totalAmountWithSubCategory) }
            .toMap()
//            .toSortedMap { pc1, pc2 ->
//                val comparatorValue =
//                    (pc2.totalAmountWithSubCategory - pc1.totalAmountWithSubCategory).toInt()
//
//                if (pieChartMode == PieChartMode.BALANCE)
//                    abs(comparatorValue)
//                else
//                    comparatorValue
//            }
    }

    private fun List<CategoryStats>.toSubCategoryData(
        allCategoriesTotalAmount: Double,
        totalCategoryAmountWithSubCategory: Double,
        pieChartMode: PieChartMode
    ): List<PieChartData> {
        return this.map { catStat ->
            val catAmt = catStat.stats.amtFromPieChartMode(pieChartMode)
            PieChartData(
                category = catStat.category,
                categoryAmount = catAmt,
                totalAmountWithSubCategory = catAmt,
                percentOfTotalAmount = (catAmt / allCategoriesTotalAmount).percent()
                    .roundTo2Digits(),
                percentOfTotalAmountWithSubCategory = (catAmt / allCategoriesTotalAmount).percent()
                    .roundTo2Digits(),

                percentShareWithInParentCategory = (catAmt / totalCategoryAmountWithSubCategory).percent()
                    .roundTo2Digits(),
                isParentCategory = false,
                show = false,
                associatedTransactions = catStat.associatedTransactions
            )
        }
    }

    private fun Double.percent() = this * 100.0

    data class Parameters(
        val transactions: List<TransactionNew>,
        val selectedAccounts: List<Account>,
        val stats: Stats,
        val pieChartMode: PieChartMode = PieChartMode.EXPENSE
    )

    data class Output(
        val pieChartDataPoints: Map<PieChartData, List<PieChartData>>,
        val stats: Stats
    )

    private data class CategoryStats(
        val category: Category,
        val stats: Stats,
        val associatedTransactions: List<TransactionNew> = emptyList()
    )
}


