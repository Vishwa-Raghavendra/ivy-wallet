package com.ivy.wallet.core.model

import com.ivy.wallet.domain.data.TransactionHistoryItem
import com.ivy.wallet.domain.data.TransactionType
import com.ivy.wallet.domain.data.core.Account
import com.ivy.wallet.domain.data.core.Category
import java.time.LocalDateTime
import java.util.*

data class TransactionNew(
    val id: UUID = UUID.randomUUID(),

    val title: String? = null,
    val description: String? = null,
    val amount: Double,
    val type: TransactionType,
    val dateTime: LocalDateTime? = null,

    val categoryId: UUID? = null,
    val category: Category? = null,

    val accountId: UUID,
    val account: Account,

    val toAccountId: UUID? = null,
    val toAccount: Account? = null,
    val toAmount: Double = amount,

    //Planned Payments
    val dueDate: LocalDateTime? = null,

    //This refers to the loan id that is linked with a transaction
    val loanId: UUID? = null,
    //This refers to the loan record id that is linked with a transaction
    val loanRecordId: UUID? = null,


    //Sync With Online //TODO(Vishwa): Remove These and replace them with gdrive Integration
    val isSynced: Boolean = false,
    val isDeleted: Boolean = false,

    //For Imports with other Apps //TODO(Vishwa): Remove These
    val recurringRuleId: UUID? = null,
    val attachmentUrl: String? = null,

    val tags : List<Tag> = emptyList()
) : TransactionHistoryItem