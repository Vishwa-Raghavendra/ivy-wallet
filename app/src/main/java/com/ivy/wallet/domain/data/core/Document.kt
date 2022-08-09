package com.ivy.wallet.domain.data.core

import com.ivy.wallet.io.persistence.data.DocumentEntity
import java.util.*

data class Document(
    val associatedId: UUID,
    val filePath: String,
    val fileName: String,
    val id: UUID = UUID.randomUUID()
) {
    fun toEntity(): DocumentEntity =
        DocumentEntity(
            associatedId = associatedId,
            filePath = filePath,
            id = id,
            fileName = fileName
        )
}
