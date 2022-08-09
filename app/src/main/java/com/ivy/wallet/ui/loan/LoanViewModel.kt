package com.ivy.wallet.ui.loan

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ivy.frp.test.TestIdlingResource
import com.ivy.wallet.domain.action.account.AccountsAct
import com.ivy.wallet.domain.action.category.CategoriesAct
import com.ivy.wallet.domain.action.edit.DocumentsLogic
import com.ivy.wallet.domain.action.loan.LoansAct
import com.ivy.wallet.domain.data.core.Account
import com.ivy.wallet.domain.data.core.Document
import com.ivy.wallet.domain.data.core.Loan
import com.ivy.wallet.domain.deprecated.logic.AccountCreator
import com.ivy.wallet.domain.deprecated.logic.LoanCreator
import com.ivy.wallet.domain.deprecated.logic.loantrasactions.LoanTransactionsLogic
import com.ivy.wallet.domain.deprecated.logic.model.CreateAccountData
import com.ivy.wallet.domain.deprecated.logic.model.CreateLoanData
import com.ivy.wallet.domain.deprecated.sync.item.LoanSync
import com.ivy.wallet.domain.event.AccountsUpdatedEvent
import com.ivy.wallet.io.persistence.SharedPrefs
import com.ivy.wallet.io.persistence.dao.AccountDao
import com.ivy.wallet.io.persistence.dao.LoanDao
import com.ivy.wallet.io.persistence.dao.LoanRecordDao
import com.ivy.wallet.io.persistence.dao.SettingsDao
import com.ivy.wallet.ui.documents.DocumentState
import com.ivy.wallet.ui.loan.data.DisplayLoan
import com.ivy.wallet.ui.theme.modal.LoanModalData
import com.ivy.wallet.utils.format
import com.ivy.wallet.utils.getDefaultFIATCurrency
import com.ivy.wallet.utils.ioThread
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import java.util.*
import javax.inject.Inject

@HiltViewModel
class LoanViewModel @Inject constructor(
    private val loanDao: LoanDao,
    private val loanRecordDao: LoanRecordDao,
    private val settingsDao: SettingsDao,
    private val loanSync: LoanSync,
    private val loanCreator: LoanCreator,
    private val sharedPrefs: SharedPrefs,
    private val accountDao: AccountDao,
    private val accountCreator: AccountCreator,
    private val loanTransactionsLogic: LoanTransactionsLogic,
    private val loansAct: LoansAct,
    private val accountsAct: AccountsAct,
    private val categoriesAct: CategoriesAct,
    private val documentsLogic: DocumentsLogic
) : ViewModel() {

    private val _baseCurrencyCode = MutableStateFlow(getDefaultFIATCurrency().currencyCode)
    val baseCurrencyCode = _baseCurrencyCode.asStateFlow()

    private val _loans = MutableStateFlow(emptyList<DisplayLoan>())
    val loans = _loans.asStateFlow()

    private val _accounts = MutableStateFlow<List<Account>>(emptyList())
    val accounts = _accounts.asStateFlow()

    private val _selectedAccount = MutableStateFlow<Account?>(null)
    val selectedAccount = _selectedAccount.asStateFlow()

    private var defaultCurrencyCode = ""

    private val _state = MutableStateFlow(LoanScreenState())
    val state: StateFlow<LoanScreenState> = _state

    private var loanId: UUID = UUID.randomUUID()

    fun start() {
        viewModelScope.launch(Dispatchers.Default) {
            TestIdlingResource.increment()

            defaultCurrencyCode = ioThread {
                settingsDao.findFirst().currency
            }.also {
                _baseCurrencyCode.value = it
            }

            initialiseAccounts()

            _loans.value = ioThread {
                loansAct(Unit)
                    .map { loan ->
                        val amountPaid = calculateAmountPaid(loan)
                        val loanAmount = loan.amount
                        val percentPaid = amountPaid / loanAmount
                        val currCode = findCurrencyCode(accounts.value, loan.accountId)

                        DisplayLoan(
                            loan = loan,
                            amountPaid = amountPaid,
                            currencyCode = currCode,
                            formattedDisplayText = "${amountPaid.format(currCode)} $currCode / ${
                                loanAmount.format(
                                    currCode
                                )
                            } $currCode (${
                                percentPaid.times(
                                    100
                                ).format(2)
                            }%)",
                            percentPaid = percentPaid
                        )
                    }
            }
            _state.value = LoanScreenState(
                baseCurrency = defaultCurrencyCode,
                loans = _loans.value,
                accounts = accounts.value,
                selectedAccount = selectedAccount.value
            )

            TestIdlingResource.decrement()
        }
    }

    private suspend fun initialiseAccounts() {
        val accounts = accountsAct(Unit)
        _accounts.value = accounts
        _selectedAccount.value = defaultAccountId(accounts)
        _selectedAccount.value?.let {
            _baseCurrencyCode.value = it.currency ?: defaultCurrencyCode
        }
    }

    fun createLoan(data: CreateLoanData) {
        viewModelScope.launch {
            TestIdlingResource.increment()

            val uuid = loanCreator.create(data, loanId = loanId) {
                start()
            }

            uuid?.let {
                loanTransactionsLogic.Loan.createAssociatedLoanTransaction(data = data, loanId = it)
            }

            TestIdlingResource.decrement()
        }
    }

    fun reorder(newOrder: List<DisplayLoan>) {
        viewModelScope.launch {
            TestIdlingResource.increment()

            ioThread {
                newOrder.forEachIndexed { index, item ->
                    loanDao.save(
                        item.loan.toEntity().copy(
                            orderNum = index.toDouble(),
                            isSynced = false
                        )
                    )
                }
            }
            start()

            ioThread {
                loanSync.sync()
            }

            TestIdlingResource.decrement()
        }
    }

    fun createAccount(data: CreateAccountData) {
        viewModelScope.launch {
            TestIdlingResource.increment()

            accountCreator.createAccount(data) {
                EventBus.getDefault().post(AccountsUpdatedEvent())
                _accounts.value = accountsAct(Unit)
                _state.value = state.value.copy(accounts = _accounts.value)
            }

            TestIdlingResource.decrement()
        }
    }

    private fun defaultAccountId(
        accounts: List<Account>,
    ): Account? {

        val lastSelectedId =
            sharedPrefs.getString(SharedPrefs.LAST_SELECTED_ACCOUNT_ID, null)?.let {
                UUID.fromString(it)
            }

        lastSelectedId?.let { uuid ->
            return accounts.find { it.id == uuid }
        } ?: run {
            return if (accounts.isNotEmpty()) accounts[0] else null
        }
    }

    private fun findCurrencyCode(accounts: List<Account>, accountId: UUID?): String {
        return accountId?.let {
            accounts.find { account -> account.id == it }?.currency
        } ?: defaultCurrencyCode
    }

    private suspend fun calculateAmountPaid(loan: Loan): Double {
        val loanRecords = ioThread { loanRecordDao.findAllByLoanId(loanId = loan.id) }
        var amount = 0.0

        loanRecords.forEach { loanRecord ->
            if (!loanRecord.interest) {
                val convertedAmount = loanRecord.convertedAmount ?: loanRecord.amount
                amount += convertedAmount
            }
        }

        return amount
    }

    private suspend fun addDocument(
        documentFileName: String,
        documentURI: Uri?,
        context: Context
    ) {
        if (documentURI == null) return

        documentsLogic.addDocument(
            documentFileName = documentFileName,
            associatedId = loanId,
            documentURI = documentURI,
            context = context,
            onProgressStart = {
                val docState = _state.value.documentState.enableProgressAndGet()
                _state.value = _state.value.copy(documentState = docState)
            },
            onProgressEnd = {
                val docState = _state.value.documentState.disableProgressAndGet()
                _state.value = _state.value.copy(documentState = docState)
            }
        )

        updateDocumentsList()
    }

    fun onEvent(event: LoanScreenEvent) {
        viewModelScope.launch(Dispatchers.Default) {
            when (event) {
                is LoanScreenEvent.OnLoanCreate -> {
                    createLoan(event.createLoanData)
                }
                is LoanScreenEvent.OnAddLoan -> {
                    _state.value = _state.value.copy(
                        loanModalData = LoanModalData(
                            loan = null,
                            baseCurrency = baseCurrencyCode.value,
                            selectedAccount = selectedAccount.value
                        )
                    )
                    updateLoanId()
                }
                is LoanScreenEvent.OnLoanModalDismiss -> {
                    _state.value = _state.value.copy(
                        loanModalData = null
                    )
                }
                is LoanScreenEvent.OnReOrderModalShow -> {
                    _state.value = _state.value.copy(
                        reorderModalVisible = event.show
                    )
                }
                is LoanScreenEvent.OnReordered -> {
                    reorder(event.reorderedList)
                    _state.value = _state.value.copy(
                        loans = event.reorderedList
                    )
                }
                is LoanScreenEvent.OnCreateAccount -> {
                    createAccount(event.accountData)
                }
                is LoanScreenEvent.OnDocumentAdd -> {
                    addDocument(event.documentFileName, event.documentURI, event.context)
                }
                is LoanScreenEvent.OnDocumentRename -> {
                    renameDocument(event.newFileName,event.document,event.context)
                }
                is LoanScreenEvent.OnDocumentDelete -> {
                    deleteDocument(event.document)
                }
            }
        }
    }

    private suspend fun deleteDocument(document: Document) {
        documentsLogic.deleteDocument(document)
        updateDocumentsList()
    }


    private suspend fun updateDocumentsList(){
        //Update Document List
        _state.value = _state.value.copy(
            documentState = _state.value.documentState.copy(
                documentList = documentsLogic.findByAssociatedId(
                    loanId
                )
            )
        )
    }

    private suspend fun renameDocument(newFileName: String, document: Document, context: Context) {
        documentsLogic.renameDocument(context, document, newFileName)
        updateDocumentsList()
    }

    private fun updateLoanId() {
        loanId = UUID.randomUUID()
    }
}

data class LoanScreenState(
    val baseCurrency: String = "",
    val loans: List<DisplayLoan> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val selectedAccount: Account? = null,
    val loanModalData: LoanModalData? = null,
    val reorderModalVisible: Boolean = false,
    val documentState: DocumentState = DocumentState.empty()
)

sealed class LoanScreenEvent {
    data class OnLoanCreate(val createLoanData: CreateLoanData) : LoanScreenEvent()
    data class OnReordered(val reorderedList: List<DisplayLoan>) : LoanScreenEvent()
    data class OnCreateAccount(val accountData: CreateAccountData) : LoanScreenEvent()
    data class OnReOrderModalShow(val show: Boolean) : LoanScreenEvent()

    data class OnDocumentAdd(
        val documentFileName: String,
        val documentURI: Uri?,
        val context: Context
    ) : LoanScreenEvent()

    data class OnDocumentRename(
        val document: Document,
        val newFileName: String,
        val context: Context
    ) : LoanScreenEvent()

    data class OnDocumentDelete(val document: Document) : LoanScreenEvent()

    object OnAddLoan : LoanScreenEvent()
    object OnLoanModalDismiss : LoanScreenEvent()
}