package com.ivy.wallet.io.persistence.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ivy.wallet.core.domain.io.MetadataProperties
import com.ivy.wallet.core.model.MetadataDomain
import java.util.*

@Entity(tableName = "metadata")
data class MetadataEntity(
    @PrimaryKey
    val id: UUID = UUID.randomUUID(),
    val associatedId: UUID,
    val property: MetadataProperties = MetadataProperties.NONE,
    val value: String = ""
) {
    fun toDomain(): MetadataDomain {
        return MetadataDomain(id, associatedId, property, value)
    }
}