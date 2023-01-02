package com.ivy.wallet.core.model

import com.ivy.wallet.io.persistence.data.TagTransactionEntity
import java.util.*


data class TagTransaction(
    val tagId:UUID,
    val associatedId:UUID,
){
    fun toEntity() : TagTransactionEntity{
        return TagTransactionEntity(tagId, associatedId)
    }
}