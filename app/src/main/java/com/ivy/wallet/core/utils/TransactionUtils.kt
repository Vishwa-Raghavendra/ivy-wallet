package com.ivy.wallet.core.utils

import com.ivy.wallet.core.model.TransactionNew
import com.ivy.wallet.domain.data.core.Account
import com.ivy.wallet.domain.data.core.Category
import com.ivy.wallet.domain.data.core.Transaction
import com.ivy.wallet.io.persistence.data.TransactionEntity


fun TransactionEntity.toTransactionDomain(
    account: Account,
    toAccount: Account? = null,
    category: Category? = null
): TransactionNew {
    val transactionDateInLocalTime = this.dateTime.fromUtcToLocalTimeNew()
    val dueDateInLocalTime = this.dueDate.fromUtcToLocalTimeNew()

    return TransactionNew(
        id = id,

        title = title,
        description = description,
        amount = amount,
        type = type,
        dateTime = transactionDateInLocalTime,

        categoryId = categoryId,
        category = category,

        accountId = accountId,
        account = account,

        toAccountId = toAccountId,
        toAccount = toAccount,
        toAmount = toAmount ?: amount,

        dueDate = dueDateInLocalTime,

        loanId = loanId,
        loanRecordId = loanRecordId,


        recurringRuleId = recurringRuleId,
        attachmentUrl = attachmentUrl
    )
}

fun TransactionNew.toOldDomain(): Transaction {
    return Transaction(
        accountId = accountId,
        type = type,
        amount = amount.toBigDecimal(),
        toAccountId = toAccountId,
        toAmount = toAmount.toBigDecimal() ?: amount.toBigDecimal(),
        title = title,
        description = description,
        dateTime = dateTime,
        categoryId = categoryId,
        dueDate = dueDate,
        recurringRuleId = recurringRuleId,
        attachmentUrl = attachmentUrl,
        loanId = loanId,
        loanRecordId = loanRecordId,
        id = id
    )
}