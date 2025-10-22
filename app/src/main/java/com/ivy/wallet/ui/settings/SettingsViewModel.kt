package com.ivy.wallet.ui.settings

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ivy.frp.monad.Res
import com.ivy.frp.test.TestIdlingResource
import com.ivy.frp.view.navigation.Navigation
import com.ivy.wallet.domain.action.global.StartDayOfMonthAct
import com.ivy.wallet.domain.action.global.UpdateStartDayOfMonthAct
import com.ivy.wallet.domain.action.transaction.FetchAllTrnsFromServerAct
import com.ivy.wallet.domain.data.TransactionType
import com.ivy.wallet.domain.data.analytics.AnalyticsEvent
import com.ivy.wallet.domain.data.core.User
import com.ivy.wallet.domain.deprecated.logic.LogoutLogic
import com.ivy.wallet.domain.deprecated.logic.csv.ExportCSVLogic
import com.ivy.wallet.domain.deprecated.logic.currency.ExchangeRatesLogic
import com.ivy.wallet.domain.deprecated.logic.zip.ExportZipLogic
import com.ivy.wallet.domain.deprecated.sync.IvySync
import com.ivy.wallet.io.SmsMessage
import com.ivy.wallet.io.SmsReader
import com.ivy.wallet.io.network.FCMClient
import com.ivy.wallet.io.network.IvyAnalytics
import com.ivy.wallet.io.network.IvySession
import com.ivy.wallet.io.network.RestClient
import com.ivy.wallet.io.network.request.auth.GoogleSignInRequest
import com.ivy.wallet.io.network.request.github.OpenIssueRequest
import com.ivy.wallet.io.persistence.SharedPrefs
import com.ivy.wallet.io.persistence.dao.SettingsDao
import com.ivy.wallet.io.persistence.dao.UserDao
import com.ivy.wallet.refreshWidget
import com.ivy.wallet.ui.IvyWalletCtx
import com.ivy.wallet.ui.Main
import com.ivy.wallet.ui.RootActivity
import com.ivy.wallet.ui.widget.WalletBalanceReceiver
import com.ivy.wallet.utils.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsDao: SettingsDao,
    private val ivySession: IvySession,
    private val userDao: UserDao,
    private val ivyContext: IvyWalletCtx,
    private val ivySync: IvySync,
    private val exportCSVLogic: ExportCSVLogic,
    private val restClient: RestClient,
    private val fcmClient: FCMClient,
    private val ivyAnalytics: IvyAnalytics,
    private val exchangeRatesLogic: ExchangeRatesLogic,
    private val logoutLogic: LogoutLogic,
    private val sharedPrefs: SharedPrefs,
    private val exportZipLogic: ExportZipLogic,
    private val startDayOfMonthAct: StartDayOfMonthAct,
    private val updateStartDayOfMonthAct: UpdateStartDayOfMonthAct,
    private val fetchAllTrnsFromServerAct: FetchAllTrnsFromServerAct,
    private val smsReader: SmsReader,
    private val nav: Navigation
) : ViewModel() {

    private val _user = MutableLiveData<User?>()
    val user = _user.asLiveData()

    private val _nameLocalAccount = MutableLiveData<String?>()
    val nameLocalAccount = _nameLocalAccount.asLiveData()

    private val _opSync = MutableLiveData<OpResult<Boolean>>()
    val opSync = _opSync.asLiveData()

    private val _currencyCode = MutableLiveData<String>()
    val currencyCode = _currencyCode.asLiveData()

    private val _lockApp = MutableLiveData<Boolean>()
    val lockApp = _lockApp.asLiveData()

    private val _hideCurrentBalance = MutableStateFlow(false)
    val hideCurrentBalance = _hideCurrentBalance.asStateFlow()

    private val _showNotifications = MutableStateFlow(true)
    val showNotifications = _showNotifications.asStateFlow()

    private val _treatTransfersAsIncomeExpense = MutableStateFlow(false)
    val treatTransfersAsIncomeExpense = _treatTransfersAsIncomeExpense.asStateFlow()

    private val _progressState = MutableStateFlow(false)
    val progressState = _progressState.asStateFlow()

    private val _startDateOfMonth = MutableLiveData<Int>()
    val startDateOfMonth = _startDateOfMonth

    private val _opFetchtrns = MutableStateFlow<OpResult<Unit>?>(null)
    val opFetchTrns = _opFetchtrns.asStateFlow()

    private val _smsMessages = MutableStateFlow<List<SmsMessage>>(emptyList())
    val smsMessages = _smsMessages.asStateFlow()

    private val _smsImportStartDate = MutableLiveData<Date?>()
    val smsImportStartDate = _smsImportStartDate.asLiveData()

    fun start() {
        viewModelScope.launch {
            TestIdlingResource.increment()

            val settings = ioThread { settingsDao.findFirst() }

            _nameLocalAccount.value = settings.name

            _startDateOfMonth.value = startDayOfMonthAct(Unit)!!

            _user.value = ioThread {
                val userId = ivySession.getUserIdSafe()
                if (userId != null) userDao.findById(userId)?.toDomain() else null
            }
            _currencyCode.value = settings.currency

            _lockApp.value = sharedPrefs.getBoolean(SharedPrefs.APP_LOCK_ENABLED, false)
            _hideCurrentBalance.value =
                sharedPrefs.getBoolean(SharedPrefs.HIDE_CURRENT_BALANCE, false)

            _showNotifications.value = sharedPrefs.getBoolean(SharedPrefs.SHOW_NOTIFICATIONS, true)

            _treatTransfersAsIncomeExpense.value =
                sharedPrefs.getBoolean(SharedPrefs.TRANSFERS_AS_INCOME_EXPENSE, false)

            _opSync.value = OpResult.success(ioThread { ivySync.isSynced() })

            TestIdlingResource.decrement()
        }
    }

    fun sync() {
        viewModelScope.launch {
            TestIdlingResource.increment()

            _opSync.value = OpResult.loading()

            ioThread {
                ivySync.sync()
            }

            _opSync.value = OpResult.success(ioThread { ivySync.isSynced() })

            TestIdlingResource.decrement()
        }
    }


    fun setName(newName: String) {
        viewModelScope.launch {
            TestIdlingResource.increment()

            ioThread {
                settingsDao.save(
                    settingsDao.findFirst().copy(
                        name = newName
                    )
                )
            }
            start()

            TestIdlingResource.decrement()
        }
    }

    fun setCurrency(newCurrency: String) {
        viewModelScope.launch {
            TestIdlingResource.increment()

            ioThread {
                settingsDao.save(
                    settingsDao.findFirst().copy(
                        currency = newCurrency
                    )
                )

                exchangeRatesLogic.sync(baseCurrency = newCurrency)
            }
            start()

            TestIdlingResource.decrement()
        }
    }

    fun exportToCSV(context: Context) {
        ivyContext.createNewFile(
            "Ivy Wallet (${
                timeNowUTC().formatNicelyWithTime(noWeekDay = true)
            }).csv"
        ) { fileUri ->
            viewModelScope.launch {
                TestIdlingResource.increment()

                exportCSVLogic.exportToFile(
                    context = context,
                    fileUri = fileUri
                )

                (context as RootActivity).shareCSVFile(
                    fileUri = fileUri
                )

                TestIdlingResource.decrement()
            }
        }
    }

    fun exportToZip(context: Context) {
        ivyContext.createNewFile(
            "Ivy Wallet (${
                timeNowUTC().formatNicelyWithTime(noWeekDay = true)
            }).zip"
        ) { fileUri ->
            viewModelScope.launch(Dispatchers.IO) {
                TestIdlingResource.increment()

                _progressState.value = true
                exportZipLogic.exportToFile(context = context, zipFileUri = fileUri)
                _progressState.value = false

                uiThread {
                    (context as RootActivity).shareZipFile(
                        fileUri = fileUri
                    )
                }

                TestIdlingResource.decrement()
            }
        }
    }


    fun setStartDateOfMonth(startDate: Int) {
        viewModelScope.launch {
            TestIdlingResource.increment()

            when (val res = updateStartDayOfMonthAct(startDate)) {
                is Res.Err -> {}
                is Res.Ok -> {
                    _startDateOfMonth.value = res.data!!
                }
            }

            TestIdlingResource.decrement()
        }
    }

    fun logout() {
        viewModelScope.launch {
            TestIdlingResource.increment()

            logoutLogic.logout()

            TestIdlingResource.decrement()
        }
    }

    fun cloudLogout() {
        viewModelScope.launch {
            TestIdlingResource.increment()

            logoutLogic.cloudLogout()

            TestIdlingResource.decrement()
        }
    }

    fun login() {
        ivyContext.googleSignIn { idToken ->
            if (idToken != null) {
                viewModelScope.launch {
                    TestIdlingResource.increment()

                    try {
                        val authResponse = restClient.authService.googleSignIn(
                            GoogleSignInRequest(
                                googleIdToken = idToken,
                                fcmToken = fcmClient.fcmToken()
                            )
                        )

                        ioThread {
                            ivySession.initiate(authResponse)

                            settingsDao.save(
                                settingsDao.findFirst().copy(
                                    name = authResponse.user.firstName
                                )
                            )
                        }

                        start()

                        ioThread {
                            ivyAnalytics.logEvent(AnalyticsEvent.LOGIN_FROM_SETTINGS)
                        }

                        sync()
                    } catch (e: Exception) {
                        e.sendToCrashlytics("Settings - GOOGLE_SIGN_IN ERROR: generic exception when logging with GOOGLE")
                        e.printStackTrace()
                        Timber.e("Settings - Login with Google failed on Ivy server - ${e.message}")
                    }

                    TestIdlingResource.decrement()
                }
            } else {
                sendToCrashlytics("Settings - GOOGLE_SIGN_IN ERROR: idToken is null!!")
                Timber.e("Settings - Login with Google failed while getting idToken")
            }
        }
    }

    fun setLockApp(lockApp: Boolean) {
        viewModelScope.launch {
            TestIdlingResource.increment()

            sharedPrefs.putBoolean(SharedPrefs.APP_LOCK_ENABLED, lockApp)
            _lockApp.value = lockApp
            refreshWidget(WalletBalanceReceiver::class.java)

            TestIdlingResource.decrement()
        }
    }

    fun setShowNotifications(showNotifications: Boolean) {
        viewModelScope.launch {
            TestIdlingResource.increment()

            sharedPrefs.putBoolean(SharedPrefs.SHOW_NOTIFICATIONS, showNotifications)
            _showNotifications.value = showNotifications

            TestIdlingResource.decrement()
        }
    }

    fun setHideCurrentBalance(hideCurrentBalance: Boolean) {
        viewModelScope.launch {
            TestIdlingResource.increment()

            sharedPrefs.putBoolean(SharedPrefs.HIDE_CURRENT_BALANCE, hideCurrentBalance)
            _hideCurrentBalance.value = hideCurrentBalance

            TestIdlingResource.decrement()
        }
    }

    fun setTransfersAsIncomeExpense(treatTransfersAsIncomeExpense: Boolean) {
        viewModelScope.launch {
            TestIdlingResource.increment()

            sharedPrefs.putBoolean(
                SharedPrefs.TRANSFERS_AS_INCOME_EXPENSE,
                treatTransfersAsIncomeExpense
            )
            _treatTransfersAsIncomeExpense.value = treatTransfersAsIncomeExpense

            TestIdlingResource.decrement()
        }
    }

    fun onSmsImportStartDateSelected(date: Date) {
        _smsImportStartDate.value = date
        importFromSms(date)
    }

    fun importFromSms(startDate: Date? = null) {
        if (smsReader.hasSmsPermission()) {
            viewModelScope.launch(Dispatchers.IO) {
                _smsMessages.update { currentList ->
                    currentList + smsReader.readSmsMessages(startDate)
                }
                delay(100)
                Timber.d("%s %s", "SMS import started", smsMessages.value)
                Timber.d("%s %s", "SMS import started", SmsTransactionParser().parse(smsMessages.value))
            }
        } else {
            ivyContext.requestSmsPermission {
                viewModelScope.launch(Dispatchers.IO) {
                    _smsMessages.update { currentList ->
                        currentList + smsReader.readSmsMessages(startDate)
                    }
                }
            }
        }
    }

    fun onSmsSelected(message: SmsMessage) {
        Timber.d("SMS selected: %s", message.body)
        clearSmsMessages()
    }

    fun clearSmsMessages() {
        _smsMessages.value = emptyList()
    }

    fun requestFeature(
        rootActivity: RootActivity,
        title: String,
        body: String
    ) {
        viewModelScope.launch {
            TestIdlingResource.increment()

            try {
                val response = restClient.githubService.openIssue(
                    request = OpenIssueRequest(
                        title = title,
                        body = body,
                    )
                )

                //Returned: https://api.github.com/repos/octocat/Hello-World/issues/1347
                //Should open: https://github.com/octocat/Hello-World/issues/1347
                val issueUrl = response.url
                    .replace("api.github.com", "github.com")
                    .replace("/repos", "")

                rootActivity.openUrlInBrowser(issueUrl)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            TestIdlingResource.decrement()
        }
    }

    fun deleteAllUserData() {
        viewModelScope.launch {
            try {
                restClient.nukeService.deleteAllUserData()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            logout()
        }
    }

    fun deleteCloudUserData() {
        viewModelScope.launch {
            try {
                restClient.nukeService.deleteAllUserData()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            cloudLogout()
        }
    }

    fun fetchMissingTransactions() {
        if (opFetchTrns.value is OpResult.Loading) {
            //wait for sync to finish
            return
        }

        if (opFetchTrns.value is OpResult.Success) {
            //go to home screen
            ivyContext.setMoreMenuExpanded(expanded = false)
            nav.navigateTo(Main)
            return
        }

        viewModelScope.launch {
            _opFetchtrns.value = OpResult.loading()

            when (val res = fetchAllTrnsFromServerAct(Unit)) {
                is Res.Ok -> _opFetchtrns.value = OpResult.success(Unit)
                is Res.Err -> _opFetchtrns.value = OpResult.failure(res.error)
            }
        }
    }
}

class SmsTransactionParser {

    private val accountRegex = Regex("""A/c\s*[\*\w-]+""", RegexOption.IGNORE_CASE)
    private val amountRegex = Regex("""(?:Rs\.?|INR)\s*([\d,]+(?:\.\d{1,2})?)""", RegexOption.IGNORE_CASE)
    private val debitKeywords = listOf("debited", "withdrawn", "spent", "sent", "transfer to")
    private val creditKeywords = listOf("credited", "received", "deposit", "refunded")
    private val titleRegex = Regex("""to\s+([A-Z0-9&\s\.\-]+)""", RegexOption.IGNORE_CASE)

    fun parse(messages: List<SmsMessage>): List<TransactionMessage> {
        return messages.map { parse(it.body) }
    }

    fun parse(message: String): TransactionMessage {
        val accountId = accountRegex.find(message)?.value
            ?.substringAfter("A/c")
            ?.trim()

        val amount = amountRegex.find(message)?.groupValues?.get(1)
            ?.replace(",", "")
            ?.toDoubleOrNull()

        val type = when {
            debitKeywords.any { message.contains(it, ignoreCase = true) } -> TransactionType.EXPENSE
            creditKeywords.any { message.contains(it, ignoreCase = true) } -> TransactionType.INCOME
            else -> TransactionType.EXPENSE
        }

        val title = titleRegex.find(message)?.groupValues?.get(1)?.trim()

        return TransactionMessage(
            accountId = accountId,
            transactionType = type,
            amount = amount,
            title = title,
            description = message
        )
    }
}

data class TransactionMessage(
    val accountId: String?,
    val transactionType: TransactionType,
    val amount: Double?,
    val title: String?,
    val description: String
)
