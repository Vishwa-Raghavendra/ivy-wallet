package com.ivy.wallet.io.persistence.data

import androidx.room.Entity
import com.ivy.wallet.core.model.TagTransaction
import java.util.*

@Entity(tableName = "tags_transaction", primaryKeys = ["tagId","associatedId"])
data class TagTransactionEntity(
    val tagId:UUID,
    val associatedId:UUID,
) {
    fun toDomain(): TagTransaction {
        return TagTransaction(tagId, associatedId)
    }
}