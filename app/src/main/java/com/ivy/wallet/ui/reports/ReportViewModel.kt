package com.ivy.wallet.ui.reports

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.ivy.frp.view.navigation.Navigation
import com.ivy.frp.viewmodel.FRPViewModel
import com.ivy.frp.viewmodel.readOnly
import com.ivy.wallet.core.data.repository.TagsRepository
import com.ivy.wallet.core.data.repository.TransactionRepository
import com.ivy.wallet.core.domain.CalculateStatsNew
import com.ivy.wallet.core.domain.CollapseGroupTransaction
import com.ivy.wallet.core.domain.ExchangeActNew
import com.ivy.wallet.core.domain.GroupTransactionsAct
import com.ivy.wallet.core.model.Tag
import com.ivy.wallet.core.model.TransactionNew
import com.ivy.wallet.core.utils.getDateTimeComparator
import com.ivy.wallet.core.utils.statsDummy
import com.ivy.wallet.core.utils.toOldDomain
import com.ivy.wallet.core.utils.unSpecifiedCategory
import com.ivy.wallet.domain.action.account.AccountsAct
import com.ivy.wallet.domain.action.category.CategoriesAct
import com.ivy.wallet.domain.action.settings.BaseCurrencyAct
import com.ivy.wallet.domain.data.TransactionType
import com.ivy.wallet.domain.data.core.Account
import com.ivy.wallet.domain.data.core.Category
import com.ivy.wallet.domain.data.core.Transaction
import com.ivy.wallet.domain.deprecated.logic.csv.ExportCSVLogic
import com.ivy.wallet.domain.pure.data.IncomeExpenseTransferPair
import com.ivy.wallet.ui.IvyWalletCtx
import com.ivy.wallet.ui.RootActivity
import com.ivy.wallet.ui.onboarding.model.TimePeriod
import com.ivy.wallet.ui.paywall.PaywallReason
import com.ivy.wallet.ui.tags.TagState
import com.ivy.wallet.utils.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

@HiltViewModel
class ReportViewModel @Inject constructor(
    private val ivyContext: IvyWalletCtx,
    private val nav: Navigation,
    private val exportCSVLogic: ExportCSVLogic,
    private val accountsAct: AccountsAct,
    private val categoriesAct: CategoriesAct,
    private val baseCurrencyAct: BaseCurrencyAct,
    private val transactionRepository: TransactionRepository,
    private val exchangeRateNew: ExchangeActNew,
    private val calculateStatsNew: CalculateStatsNew,
    private val groupTransactionsAct: GroupTransactionsAct,
    private val collapseGroupTransaction: CollapseGroupTransaction,
    private val tagsRepository: TagsRepository,
) : FRPViewModel<ReportScreenState, Nothing>() {
    override val _state: MutableStateFlow<ReportScreenState> = MutableStateFlow(
        ReportScreenState()
    )

    override suspend fun handleEvent(event: Nothing): suspend () -> ReportScreenState {
        TODO("Not yet implemented")
    }

    private val _period = MutableLiveData<TimePeriod>()
    val period = _period.asLiveData()

    private val _historyIncomeExpense = MutableStateFlow(IncomeExpenseTransferPair.zero())
    private val historyIncomeExpense = _historyIncomeExpense.readOnly()

    private val _filter = MutableStateFlow<ReportFilter?>(null)
    val filter = _filter.readOnly()

    private var _filteredTransactions = listOf<TransactionNew>()

    private val selectedTags = mutableSetOf<Tag>()
    private var _tagSearchString: String = ""
    var tagSearchJob: Job? = null

    fun start() {
        viewModelScope.launch(Dispatchers.IO) {
            updateState {
                it.copy(
                    baseCurrency = baseCurrencyAct(Unit),
                    categories = listOf(unSpecifiedCategory) + categoriesAct(Unit),
                    accounts = accountsAct(Unit),
                    tagState = updateTagState(emptyList())
                )
            }
        }
    }

    private suspend fun setFilterNew(filter: ReportFilter?) {
        //Clear Filter
        filter ?: return clearFilter()

        if (!filter.validate()) return

        updateState { it.copy(loading = true) }

        _filteredTransactions = getTransactions(filter)
            .filterTransactions(filter)

        val stats = asyncIo {
            calculateStatsNew(
                CalculateStatsNew.Input(
                    transactions = _filteredTransactions,
                    selectedAccounts = filter.accounts,
                    treatTransfersAsIncomeExpense = filter.treatTransfersAsIncomeExpense
                )
            )
        }

        val groupedTransactions = asyncIo {
            groupTransactionsAct(
                GroupTransactionsAct.Input(
                    transactions = _filteredTransactions,
                    selectedAccounts = filter.accounts,
                    treatTransfersAsIncomeExpense = filter.treatTransfersAsIncomeExpense
                )
            )
        }

        updateState {
            it.copy(
                stats = stats.await(),
                historyTransactionsNew = groupedTransactions.await().history,
                upcomingTransactionsNew = groupedTransactions.await().upcoming,
                overdueTransactionsNew = groupedTransactions.await().overdue,
                loading = false,
                filter = filter,
                filterOverlayVisible = false,
                accountIdFilters = filter.accounts.map { a -> a.id },
                transactionsNew = _filteredTransactions
            )
        }
    }

    private suspend fun getTransactions(filter: ReportFilter): List<TransactionNew> {
        filter.period ?: return transactionRepository.getAllTransactions()

        val dateTimeComparator =
            filter.period.toRange(ivyContext.startDayOfMonth).getDateTimeComparator()

        return transactionRepository.findByDate(
            dateTimeComparator.getStartTimeInMilli(),
            dateTimeComparator.getEndTimeInMilli()
        )
    }

    private suspend fun clearFilter() {
        _filter.value = null
        _filteredTransactions = listOf()
        selectedTags.clear()
        updateState {
            it.copy(
                stats = statsDummy(),
                historyTransactionsNew = emptyList(),
                upcomingTransactionsNew = emptyList(),
                overdueTransactionsNew = emptyList(),
                loading = false,
                filterOverlayVisible = false,
                filter = null,
                accountIdFilters = emptyList()
            )
        }
    }

    private suspend fun List<TransactionNew>.filterTransactions(filter: ReportFilter): List<TransactionNew> {
        return this
            .filterByTransactionType(filter.trnTypes.toHashSet())
            .filterByTimePeriod(filter.period)
            .filterByAccounts(filter.accounts)
            .filterByCategories(filter.categories, unspecifiedCat = unSpecifiedCategory)
            .filterByAmount(
                baseCurr = stateVal().baseCurrency,
                minAmt = filter.minAmount,
                maxAmt = filter.maxAmount
            )
            .filterByWords(
                includeKeywords = filter.includeKeywords,
                excludeKeywords = filter.excludeKeywords
            )
            .filterPlannedPayments()
            .filterTags()
    }

    private fun filterTransactions(): List<Transaction> {

        return _filteredTransactions
            .map(TransactionNew::toOldDomain)
            .toList()
    }

    private suspend fun export(context: Context) {
        ivyContext.protectWithPaywall(
            paywallReason = PaywallReason.EXPORT_CSV,
            navigation = nav
        ) {
            val filter = stateVal().filter ?: return@protectWithPaywall
            if (!filter.validate()) return@protectWithPaywall

            ivyContext.createNewFile(
                "Report (${
                    timeNowUTC().formatNicelyWithTime(noWeekDay = true)
                }).csv"
            ) { fileUri ->
                viewModelScope.launch {
                    updateState {
                        it.copy(loading = true)
                    }

                    exportCSVLogic.exportToFile(
                        context = context,
                        fileUri = fileUri,
                        exportScope = { filterTransactions() }
                    )

                    (context as RootActivity).shareCSVFile(
                        fileUri = fileUri
                    )

                    updateState {
                        it.copy(loading = false)
                    }
                }
            }
        }
    }

    private fun setFilterOverlayVisible(filterOverlayVisible: Boolean) {
        updateStateNonBlocking {
            it.copy(filterOverlayVisible = filterOverlayVisible)
        }
    }

    private suspend fun onTreatTransfersAsIncomeExpense(treatTransfersAsIncExp: Boolean) {
        updateState {
            val income = historyIncomeExpense.value.income.toDouble() +
                    if (treatTransfersAsIncExp) historyIncomeExpense.value.transferIncome.toDouble() else 0.0
            val expenses = historyIncomeExpense.value.expense.toDouble() +
                    if (treatTransfersAsIncExp) historyIncomeExpense.value.transferExpense.toDouble() else 0.0
            it.copy(
                treatTransfersAsIncExp = treatTransfersAsIncExp,
                income = income,
                expenses = expenses
            )
        }
    }

    fun onEvent(event: ReportScreenEvent) {
        viewModelScope.launch(Dispatchers.Default) {
            when (event) {
                is ReportScreenEvent.OnFilter -> scopedIOThread {
                    setFilterNew(event.filter)
                }
                is ReportScreenEvent.OnExport -> export(event.context)
                is ReportScreenEvent.OnFilterOverlayVisible -> setFilterOverlayVisible(event.filterOverlayVisible)
                is ReportScreenEvent.OnTreatTransfersAsIncomeExpense -> onTreatTransfersAsIncomeExpense(
                    event.transfersAsIncomeExpense
                )
                is ReportScreenEvent.OnDateCollapse -> {
                    updateState { state ->
                        state.copy(
                            historyTransactionsNew = collapseGroupTransaction
                                .filterCollapsedTransactions(
                                    event.date,
                                    state.historyTransactionsNew
                                )
                        )
                    }
                }
                is ReportScreenEvent.SelectTag -> {
                    selectTag(event.tag)
                }
                is ReportScreenEvent.DeSelectTag -> {
                    deSelectTag(event.tag)
                }
                is ReportScreenEvent.OnTagSearch -> {
                    onTagSearch(event.searchString)
                }
                else -> {

                }
            }
        }
    }

    private suspend fun onTagSearch(searchString: String) {
        tagSearchJob?.cancel()
        tagSearchJob = viewModelScope.launch(Dispatchers.Default) {
            delay(500)
            _tagSearchString = searchString
            updateState {
                it.copy(
                    tagState = updateTagState(if (searchString.isEmpty()) emptyList() else it.tagState.allTags)
                )
            }
        }
    }

    private suspend fun selectTag(tag: Tag) {
        selectedTags.add(tag)
        updateState {
            it.copy(
                tagState = updateTagState(it.tagState.allTags)
            )
        }
    }

    private suspend fun deSelectTag(tag: Tag) {
        selectedTags.remove(tag)
        updateState {
            it.copy(
                tagState = updateTagState(it.tagState.allTags)
            )
        }
    }


    private fun List<TransactionNew>.filterByTransactionType(
        selectedTransactionTypes: HashSet<TransactionType>
    ): List<TransactionNew> {
        return this.filter { selectedTransactionTypes.contains(it.type) }
    }

    private fun List<TransactionNew>.filterByTimePeriod(
        timePeriod: TimePeriod?
    ): List<TransactionNew> {
        val dateTimeComparator = timePeriod?.toRange(ivyContext.startDayOfMonth)
            .getDateTimeComparator()

        return this.filter {
            dateTimeComparator.isDateInRange(it.dateTime) || dateTimeComparator.isDateInRange(it.dueDate)
        }
    }

    private fun List<TransactionNew>.filterByAccounts(
        selectedAccounts: List<Account>
    ): List<TransactionNew> {
        val selectedAccountIds = selectedAccounts.map { it.id }.toSet()

        return this.filter {
            selectedAccountIds.contains(it.accountId) ||
                    selectedAccountIds.contains(it.toAccountId)
        }
    }

    private fun List<TransactionNew>.filterByCategories(
        selectedCategories: List<Category>,
        unspecifiedCat: Category
    ): List<TransactionNew> {
        val selectedCategoryIds =
            selectedCategories.map { if (it.id == unspecifiedCat.id) null else it.id }
        return this.filter { selectedCategoryIds.contains(it.categoryId) }
    }

    private suspend fun List<TransactionNew>.filterByAmount(
        baseCurr: String,
        minAmt: Double?,
        maxAmt: Double?,
    ): List<TransactionNew> {
        suspend fun amountInBaseCurrency(
            amount: Double?,
            toCurr: String?
        ): Double {
            amount ?: return 0.0
            return exchangeRateNew.exchangeAmount(
                amount,
                fromCurrency = baseCurr,
                toCurrency = toCurr ?: baseCurr
            )
        }

        suspend fun filterTrans(
            transactionList: List<TransactionNew> = this,
            filterAmount: (Double) -> Boolean
        ): List<TransactionNew> {
            return transactionList.filter {
                val amt =
                    amountInBaseCurrency(
                        amount = it.amount,
                        toCurr = it.account.currency ?: baseCurr
                    )

                val transferTrnsValue = it.toAmount

                val toAmt = amountInBaseCurrency(
                    amount = transferTrnsValue,
                    toCurr = it.toAccount?.currency ?: baseCurr
                )

                filterAmount(amt) || filterAmount(toAmt)
            }
        }

        return when {
            minAmt != null && maxAmt != null ->
                filterTrans { amt -> amt >= minAmt && amt <= maxAmt }
            minAmt != null -> filterTrans { amt -> amt >= minAmt }
            maxAmt != null -> filterTrans { amt -> amt <= maxAmt }
            else -> {
                this
            }
        }
    }

    private fun List<TransactionNew>.filterByWords(
        includeKeywords: List<String>,
        excludeKeywords: List<String>,
    ): List<TransactionNew> {
        fun List<TransactionNew>.filterTrans(
            keyWords: List<String>,
            include: Boolean = true
        ): List<TransactionNew> {
            if (keyWords.isEmpty())
                return this

            return this.filter {
                val title = it.title ?: ""
                val description = it.description ?: ""

                keyWords.forEach { k ->
                    val key = k.trim()
                    if (title.contains(key, ignoreCase = true) || description.contains(
                            key,
                            ignoreCase = true
                        )
                    )
                        return@filter include
                }

                !include
            }
        }

        return this
            .filterTrans(includeKeywords)
            .filterTrans(excludeKeywords, include = false)
    }

    private fun List<TransactionNew>.filterPlannedPayments() =
        this.filter { it.dateTime != null }

    private fun List<TransactionNew>.filterTags() =
        if (selectedTags.isEmpty())
            this
        else
            this.filter {
                it.tags.any { tag -> selectedTags.contains(tag) }
            }

    private suspend fun updateTagState(
        givenTags: List<Tag>,
    ): TagState {
        val transactionTags = selectedTags

        val allTags = (
                if (givenTags.isNotEmpty())
                    givenTags
                else
                    tagsRepository.getAllTags()
                )
            .filter {
                if (_tagSearchString.isNotNullOrBlank()) {
                    it.name.contains(_tagSearchString, ignoreCase = true)
                } else
                    true
            }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
            .sortedByDescending { transactionTags.contains(it) }

        return TagState(
            allTags,
            transactionTags.toSet(),
            chunkedAllTags = allTags.chunked(2)
        )
    }
}
