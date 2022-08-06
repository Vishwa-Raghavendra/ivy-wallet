package com.ivy.wallet.ui.edit

data class DocumentState(
    val existingDocumentList: List<String> = emptyList(),
    val showProgress: Boolean = false
)