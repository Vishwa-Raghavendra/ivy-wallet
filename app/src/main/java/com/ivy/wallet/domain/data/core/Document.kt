package com.ivy.wallet.domain.data.core

import com.ivy.wallet.io.persistence.data.DocumentEntity
import java.util.*

data class Document(
    val transactionId: UUID,
    val filePath: String,
    val fileName: String,
    val id: UUID = UUID.randomUUID()
) {
    fun toEntity(): DocumentEntity =
        DocumentEntity(
            transactionId = transactionId,
            filePath = filePath,
            id = id,
            fileName = fileName
        )
}
