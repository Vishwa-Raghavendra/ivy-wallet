package com.ivy.wallet.io.persistence.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ivy.wallet.domain.data.core.Document
import java.util.*

@Entity(tableName = "documents")
data class DocumentEntity(
    val associatedId: UUID,
    val filePath: String,
    val fileName: String,


    @PrimaryKey
    val id: UUID = UUID.randomUUID(),
) {
    fun toDomain(): Document =
        Document(associatedId = associatedId, filePath = filePath, id = id, fileName = fileName)
}
