package com.ivy.wallet.ui.tags

import com.ivy.wallet.core.model.Tag


data class TagState(
    val allTags: List<Tag> = emptyList(),
    val transactionTags: Set<Tag> = emptySet(),

    val chunkedAllTags: List<List<Any>> = emptyList(),
)