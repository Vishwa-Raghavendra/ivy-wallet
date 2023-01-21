package com.ivy.wallet.core.model

import androidx.room.PrimaryKey
import com.ivy.wallet.core.domain.io.MetadataProperties
import com.ivy.wallet.io.persistence.data.MetadataEntity
import java.util.*


data class MetadataDomain(
    @PrimaryKey
    val id: UUID = UUID.randomUUID(),
    val associatedId: UUID,
    val property: MetadataProperties = MetadataProperties.NONE,
    val value: String = ""
) {
    fun entity(): MetadataEntity {
        return MetadataEntity(id, associatedId, property, value)
    }
}