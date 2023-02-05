package com.ivy.wallet.domain.deprecated.logic

import android.util.Log
import androidx.compose.ui.graphics.toArgb
import com.ivy.wallet.core.data.repository.MetadataRepository
import com.ivy.wallet.core.domain.io.MetadataProperties
import com.ivy.wallet.core.utils.roundTo2Digits
import com.ivy.wallet.domain.data.core.Account
import com.ivy.wallet.domain.deprecated.logic.model.CreateAccountData
import com.ivy.wallet.domain.deprecated.sync.item.TransactionSync
import com.ivy.wallet.domain.deprecated.sync.uploader.AccountUploader
import com.ivy.wallet.domain.pure.util.nextOrderNum
import com.ivy.wallet.io.persistence.dao.AccountDao
import com.ivy.wallet.utils.asyncIo
import com.ivy.wallet.utils.ioThread
import com.ivy.wallet.utils.scopedIOThread
import kotlinx.coroutines.launch

class AccountCreator(
    private val paywallLogic: PaywallLogic,
    private val accountDao: AccountDao,
    private val accountUploader: AccountUploader,
    private val transactionSync: TransactionSync,
    private val accountLogic: WalletAccountLogic,
    private val metadataRepository: MetadataRepository,
) {

    suspend fun createAccount(
        data: CreateAccountData,
        onRefreshUI: suspend () -> Unit
    ) {
        val name = data.name
        if (name.isBlank()) return

        paywallLogic.protectAddWithPaywall(
            addAccount = true
        ) {
            val newAccount = scopedIOThread { scope ->
                val account = Account(
                    name = name,
                    currency = data.currency,
                    color = data.color.toArgb(),
                    icon = data.icon,
                    includeInBalance = data.includeBalance,
                    orderNum = accountDao.findMaxOrderNum().nextOrderNum(),
                    isSynced = false
                )
                accountDao.save(account.toEntity())
                scope.launch {
                    if (data.archiveAccount)
                        metadataRepository.addMetadata(account.id, MetadataProperties.ARCHIVED)
                }

                accountLogic.adjustBalance(
                    account = account,
                    actualBalance = 0.0,
                    newBalance = data.balance
                )
                account
            }

            onRefreshUI()

            ioThread {
                accountUploader.sync(newAccount)
                transactionSync.sync()
            }
        }
    }

    suspend fun editAccount(
        account: Account,
        newBalance: Double,
        isArchived: Boolean = false,
        onRefreshUI: suspend () -> Unit
    ) {
        val updatedAccount = account.copy(
            isSynced = false
        )

        scopedIOThread { scope ->
            scope.launch {
                if (isArchived)
                    metadataRepository.addMetadata(account.id, MetadataProperties.ARCHIVED)
                else
                    metadataRepository.deleteIndividualMetadata(
                        account.id,
                        MetadataProperties.ARCHIVED
                    )
            }

            accountDao.save(updatedAccount.toEntity())

            val actualBalance = accountLogic.calculateAccountBalance(updatedAccount)
            if (actualBalance.roundTo2Digits() != newBalance) {
                accountLogic.adjustBalance(
                    account = updatedAccount,
                    actualBalance = accountLogic.calculateAccountBalance(updatedAccount),
                    newBalance = newBalance
                )
            }
        }

        onRefreshUI()

        ioThread {
            accountUploader.sync(updatedAccount)
            transactionSync.sync()
        }
    }
}