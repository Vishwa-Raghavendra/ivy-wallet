package com.ivy.wallet.ui.loandetails

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ivy.frp.test.TestIdlingResource
import com.ivy.frp.view.navigation.Navigation
import com.ivy.wallet.core.domain.ExchangeActNew
import com.ivy.wallet.core.model.LoanRecordType
import com.ivy.wallet.domain.action.account.AccountsAct
import com.ivy.wallet.domain.action.edit.DocumentsLogic
import com.ivy.wallet.domain.action.loan.LoanByIdAct
import com.ivy.wallet.domain.data.core.*
import com.ivy.wallet.domain.deprecated.logic.AccountCreator
import com.ivy.wallet.domain.deprecated.logic.LoanCreator
import com.ivy.wallet.domain.deprecated.logic.LoanRecordCreator
import com.ivy.wallet.domain.deprecated.logic.loantrasactions.LoanTransactionsLogic
import com.ivy.wallet.domain.deprecated.logic.model.CreateAccountData
import com.ivy.wallet.domain.deprecated.logic.model.CreateLoanRecordData
import com.ivy.wallet.domain.deprecated.logic.model.EditLoanRecordData
import com.ivy.wallet.domain.event.AccountsUpdatedEvent
import com.ivy.wallet.io.persistence.dao.*
import com.ivy.wallet.ui.IvyWalletCtx
import com.ivy.wallet.ui.LoanDetails
import com.ivy.wallet.ui.documents.DocumentState
import com.ivy.wallet.ui.loan.data.DisplayLoanRecord
import com.ivy.wallet.utils.computationThread
import com.ivy.wallet.utils.ioThread
import com.ivy.wallet.utils.readOnly
import com.ivy.wallet.utils.replace
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import java.util.*
import javax.inject.Inject

@HiltViewModel
class LoanDetailsViewModel @Inject constructor(
    private val loanDao: LoanDao,
    private val loanRecordDao: LoanRecordDao,
    private val loanCreator: LoanCreator,
    private val loanRecordCreator: LoanRecordCreator,
    private val settingsDao: SettingsDao,
    private val ivyContext: IvyWalletCtx,
    private val transactionDao: TransactionDao,
    private val accountDao: AccountDao,
    private val accountCreator: AccountCreator,
    private val loanTransactionsLogic: LoanTransactionsLogic,
    private val nav: Navigation,
    private val accountsAct: AccountsAct,
    private val loanByIdAct: LoanByIdAct,
    private val documentsLogic: DocumentsLogic,
    private val exchangeActNew: ExchangeActNew,
) : ViewModel() {

    private val _baseCurrency = MutableStateFlow("")
    val baseCurrency = _baseCurrency.asStateFlow()

    private val _loan = MutableStateFlow<Loan?>(null)
    val loan = _loan.asStateFlow()

    private val _displayLoanRecords = MutableStateFlow(emptyList<DisplayLoanRecord>())
    val displayLoanRecords = _displayLoanRecords.asStateFlow()

    private val _amountPaid = MutableStateFlow(0.0)
    val amountPaid = _amountPaid.asStateFlow()

    private val _accounts = MutableStateFlow<List<Account>>(emptyList())
    val accounts = _accounts.asStateFlow()

    private val _loanInterestAmountPaid = MutableStateFlow(0.0)
    val loanAmountPaid = _loanInterestAmountPaid.asStateFlow()

    private val _selectedLoanAccount = MutableStateFlow<Account?>(null)
    val selectedLoanAccount = _selectedLoanAccount.asStateFlow()

    private var associatedTransaction: Transaction? = null

    private val _createLoanTransaction = MutableStateFlow(false)
    val createLoanTransaction = _createLoanTransaction.asStateFlow()

    private val _loanDocumentState = MutableStateFlow(DocumentState.empty())
    val loanDocumentState = _loanDocumentState.readOnly()

    private val _loanRecordDocumentState = MutableStateFlow(DocumentState.empty())
    val loanRecordDocumentState = _loanRecordDocumentState.readOnly()

    private var defaultCurrencyCode = ""
    private var loanRecordId: UUID = UUID.randomUUID()

    fun start(screen: LoanDetails) {
        load(loanId = screen.loanId)
    }

    private fun load(loanId: UUID) {
        viewModelScope.launch {
            TestIdlingResource.increment()

            defaultCurrencyCode = ioThread {
                settingsDao.findFirst().currency
            }.also {
                _baseCurrency.value = it
            }

            _accounts.value = accountsAct(Unit)

            _loan.value = loanByIdAct(loanId)

            loan.value?.let { loan ->
                _selectedLoanAccount.value = accounts.value.find {
                    loan.accountId == it.id
                }

                _selectedLoanAccount.value?.let { acc ->
                    _baseCurrency.value = acc.currency ?: defaultCurrencyCode
                }

                val loanDocuments = documentsLogic.findByAssociatedId(loan.id)
                _loanDocumentState.value = DocumentState(documentList = loanDocuments)
            }

            computationThread {
                _displayLoanRecords.value =
                    ioThread { loanRecordDao.findAllByLoanId(loanId = loanId) }.map {
                        val trans = ioThread {
                            transactionDao.findLoanRecordTransaction(
                                it.id
                            )
                        }

                        val documentList = documentsLogic.findByAssociatedId(it.id)

                        val account = findAccount(
                            accounts = accounts.value,
                            accountId = it.accountId,
                        )

                        DisplayLoanRecord(
                            it.toDomain(),
                            account = account,
                            loanRecordTransaction = trans != null,
                            loanRecordCurrencyCode = account?.currency ?: defaultCurrencyCode,
                            loanCurrencyCode = selectedLoanAccount.value?.currency
                                ?: defaultCurrencyCode,
                            loanRecordDocumentState = DocumentState(documentList = documentList)
                        )
                    }

                val increasedLoanAmount =
                    (loan.value?.amount ?: 0.0) + displayLoanRecords.value.sumOf {
                        if (it.loanRecord.loanRecordType == LoanRecordType.LOAN_INCREASE) {
                            val fromCurrency = it.account?.currency ?: defaultCurrencyCode
                            val toCurrency =
                                accounts.value.find { acc -> acc.id == loan.value?.accountId }?.currency
                                    ?: defaultCurrencyCode

                            exchangeActNew.exchangeAmount(
                                it.loanRecord.amount,
                                fromCurrency,
                                toCurrency
                            )
                        } else
                            0.0
                    }

                _loan.value = _loan.value?.copy(amount = increasedLoanAmount)
            }

            computationThread {
                //Using a local variable to calculate the amount and then reassigning to
                // the State variable to reduce the amount of compose re-draws
                var amtPaid = 0.0
                var loanInterestAmtPaid = 0.0
                displayLoanRecords.value.forEach {
                    val convertedAmount = it.loanRecord.convertedAmount ?: it.loanRecord.amount
                    if (!it.loanRecord.interest && it.loanRecord.loanRecordType == LoanRecordType.DEFAULT) {
                        amtPaid += convertedAmount
                    } else if (it.loanRecord.loanRecordType == LoanRecordType.DEFAULT)
                        loanInterestAmtPaid += convertedAmount
                }

                _amountPaid.value = amtPaid
                _loanInterestAmountPaid.value = loanInterestAmtPaid
            }

            associatedTransaction = ioThread {
                transactionDao.findLoanTransaction(loanId = loan.value!!.id)?.toDomain()
            }

            associatedTransaction?.let {
                _createLoanTransaction.value = true
            } ?: run {
                _createLoanTransaction.value = false
            }

            TestIdlingResource.decrement()
        }
    }

    fun editLoan(loan: Loan, createLoanTransaction: Boolean = false) {
        viewModelScope.launch {
            TestIdlingResource.increment()

            _loan.value?.let {
                loanTransactionsLogic.Loan.recalculateLoanRecords(
                    oldLoanAccountId = it.accountId,
                    newLoanAccountId = loan.accountId,
                    loanId = loan.id
                )
            }

            loanTransactionsLogic.Loan.editAssociatedLoanTransaction(
                loan = loan,
                createLoanTransaction = createLoanTransaction,
                transaction = associatedTransaction
            )

            loanCreator.edit(loan) {
                load(loanId = it.id)
            }

            TestIdlingResource.decrement()
        }
    }

    fun deleteLoan() {
        val loan = loan.value ?: return

        viewModelScope.launch {
            TestIdlingResource.increment()

            loanTransactionsLogic.Loan.deleteAssociatedLoanTransactions(loan.id)

            loanCreator.delete(loan) {
                //close screen
                nav.back()
            }

            TestIdlingResource.decrement()
        }
    }

    fun createLoanRecord(data: CreateLoanRecordData) {
        if (loan.value == null) return
        val loanId = loan.value?.id ?: return
        val localLoan = loan.value!!

        viewModelScope.launch {
            TestIdlingResource.increment()

            val modifiedData = data.copy(
                convertedAmount = loanTransactionsLogic.LoanRecord.calculateConvertedAmount(
                    data = data,
                    loanAccountId = localLoan.accountId
                )
            )

            val loanRecordUUID = loanRecordCreator.create(
                loanRecordId = loanRecordId,
                loanId = loanId,
                data = modifiedData
            ) {
                load(loanId = loanId)
            }

            loanRecordUUID?.let {
                loanTransactionsLogic.LoanRecord.createAssociatedLoanRecordTransaction(
                    data = modifiedData,
                    loan = localLoan,
                    loanRecordId = it
                )
            }

            TestIdlingResource.decrement()
        }
    }

    fun editLoanRecord(editLoanRecordData: EditLoanRecordData) {
        viewModelScope.launch {
            val loanRecord = editLoanRecordData.newLoanRecord
            TestIdlingResource.increment()

            val localLoan: Loan = _loan.value ?: return@launch

            val convertedAmount =
                loanTransactionsLogic.LoanRecord.calculateConvertedAmount(
                    loanAccountId = localLoan.accountId,
                    newLoanRecord = editLoanRecordData.newLoanRecord,
                    oldLoanRecord = editLoanRecordData.originalLoanRecord,
                    reCalculateLoanAmount = editLoanRecordData.reCalculateLoanAmount
                )

            val modifiedLoanRecord =
                editLoanRecordData.newLoanRecord.copy(convertedAmount = convertedAmount)

            loanTransactionsLogic.LoanRecord.editAssociatedLoanRecordTransaction(
                loan = localLoan,
                createLoanRecordTransaction = editLoanRecordData.createLoanRecordTransaction,
                loanRecord = loanRecord,
            )

            loanRecordCreator.edit(modifiedLoanRecord) {
                load(loanId = it.loanId)
            }

            TestIdlingResource.decrement()
        }
    }

    fun deleteLoanRecord(loanRecord: LoanRecord) {
        val loanId = loan.value?.id ?: return

        viewModelScope.launch {
            TestIdlingResource.increment()

            loanRecordCreator.delete(loanRecord) {
                load(loanId = loanId)
            }

            loanTransactionsLogic.LoanRecord.deleteAssociatedLoanRecordTransaction(loanRecordId = loanRecord.id)

            TestIdlingResource.decrement()
        }
    }

    fun onLoanTransactionChecked(boolean: Boolean) {
        _createLoanTransaction.value = boolean
    }

    fun createAccount(data: CreateAccountData) {
        viewModelScope.launch {
            TestIdlingResource.increment()

            accountCreator.createAccount(data) {
                EventBus.getDefault().post(AccountsUpdatedEvent())
                _accounts.value = accountsAct(Unit)
            }

            TestIdlingResource.decrement()
        }
    }

    private fun findAccount(
        accounts: List<Account>,
        accountId: UUID?,
    ): Account? {
        return accountId?.let { uuid ->
            accounts.find { acc ->
                acc.id == uuid
            }
        }
    }

    //----------------------  Loan Document Operations  ----------------------------

    fun addDocumentLoan(documentFileName: String, documentURI: Uri?, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            addDocumentInternal(
                documentFileName = documentFileName,
                documentURI = documentURI,
                context = context,
                id = loan.value?.id,
                onProgressStart = {
                    _loanDocumentState.value =
                        _loanDocumentState.value.copy(showProgress = true)
                },
                onProgressEnd = {
                    _loanDocumentState.value =
                        _loanDocumentState.value.copy(showProgress = false)
                },
                onUpdateAction = this@LoanDetailsViewModel::updateLoanDocumentsList
            )
        }
    }

    fun renameDocumentLoan(context: Context, document: Document, newFileName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            renameDocumentInternal(
                context = context,
                document = document,
                newFileName = newFileName,
                onUpdateAction = this@LoanDetailsViewModel::updateLoanDocumentsList
            )
        }
    }

    fun deleteDocumentLoan(document: Document) {
        viewModelScope.launch(Dispatchers.IO) {
            deleteDocumentInternal(
                document = document,
                onUpdateAction = this@LoanDetailsViewModel::updateLoanDocumentsList
            )
        }
    }

    //----------------------  LoanRecord Document Operations  ----------------------------

    fun addDocumentLoanRecord(
        documentFileName: String,
        documentURI: Uri?,
        context: Context,
        loanRecord: LoanRecord?
    ) {
        val id = loanRecord?.id ?: loanRecordId
        viewModelScope.launch(Dispatchers.IO) {

            var showProgress = false

            addDocumentInternal(
                documentFileName = documentFileName,
                documentURI = documentURI,
                context = context,
                id = id,
                onProgressStart = {
                    showProgress = true
                },
                onProgressEnd = {
                    showProgress = false
                },
                onUpdateAction = {
                    updateLoanRecordState(loanRecord, showProgress, loanRecordId)
                }
            )
        }
    }

    fun renameDocumentLoanRecord(
        context: Context,
        document: Document,
        newFileName: String,
        loanRecord: LoanRecord?
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            renameDocumentInternal(
                context = context,
                document = document,
                newFileName = newFileName,
                onUpdateAction = {
                    updateLoanRecordState(loanRecord, defaultId = loanRecordId)
                }
            )
        }
    }

    fun deleteDocumentLoanRecord(document: Document, loanRecord: LoanRecord?) {
        viewModelScope.launch(Dispatchers.IO) {
            deleteDocumentInternal(
                document = document,
                onUpdateAction = {
                    updateLoanRecordState(loanRecord, defaultId = loanRecordId)
                }
            )
        }
    }

    //----------------------  Generic Document Operations  ----------------------------

    private suspend fun addDocumentInternal(
        documentFileName: String,
        documentURI: Uri?,
        context: Context,
        id: UUID?,
        onProgressStart: suspend () -> Unit = {},
        onProgressEnd: suspend () -> Unit = {},
        onUpdateAction: suspend () -> Unit = {},
    ) {
        if (documentURI == null || id == null)
            return

        documentsLogic.addDocument(
            documentFileName = documentFileName,
            associatedId = id,
            documentURI = documentURI,
            context = context,
            onProgressStart = onProgressStart,
            onProgressEnd = onProgressEnd
        )

        onUpdateAction()
    }

    private suspend fun renameDocumentInternal(
        context: Context,
        document: Document,
        newFileName: String,
        onUpdateAction: suspend () -> Unit = {},
    ) {
        documentsLogic.renameDocument(context, document, newFileName)
        onUpdateAction()
    }

    private suspend fun deleteDocumentInternal(
        document: Document,
        onUpdateAction: suspend () -> Unit = {},
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            documentsLogic.deleteDocument(document)
            onUpdateAction()
        }
    }

    //----------------------  Document State Update Functions  ----------------------------

    private suspend fun updateLoanRecordState(
        loanRecord: LoanRecord?,
        showProgress: Boolean = false,
        defaultId: UUID
    ) {
        val id = loanRecord?.id ?: defaultId

        viewModelScope.launch(Dispatchers.IO) {
            val list = documentsLogic.findByAssociatedId(id)
            val newDocState = DocumentState(documentList = list, showProgress = showProgress)
            _loanRecordDocumentState.value = newDocState

            val existingDisplayLoanRecord =
                displayLoanRecords.value.find { it.loanRecord.id == id } ?: return@launch

            val newDisplayRecord =
                existingDisplayLoanRecord.copy(loanRecordDocumentState = newDocState)

            _displayLoanRecords.value = displayLoanRecords.value.replace(
                oldComp = { d -> d.loanRecord.id == newDisplayRecord.loanRecord.id },
                newDisplayRecord
            )
        }
    }

    private suspend fun updateLoanDocumentsList() {
        loan.value?.let { l ->
            _loanDocumentState.value = _loanDocumentState.value.copy(
                documentList = documentsLogic.findByAssociatedId(l.id)
            )
        }
    }

    fun updateLoanRecordDefaultId() {
        viewModelScope.launch(Dispatchers.Default) {
            loanRecordId = UUID.randomUUID()
            _loanRecordDocumentState.value = DocumentState.empty()
        }
    }

    fun updateLoanRecordDocumentState(displayLoanRecord: DisplayLoanRecord) {
        viewModelScope.launch(Dispatchers.Default) {
            _loanRecordDocumentState.value = displayLoanRecord.loanRecordDocumentState
        }
    }
}