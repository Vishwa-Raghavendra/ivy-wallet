package com.ivy.wallet.ui.edit

import com.ivy.wallet.domain.data.core.Document

data class DocumentState(
    val documentList: List<Document> = emptyList(),
    val showProgress: Boolean = false
)