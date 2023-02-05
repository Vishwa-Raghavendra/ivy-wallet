package com.ivy.wallet.core.data.repository

import com.ivy.wallet.core.domain.io.MetadataProperties
import com.ivy.wallet.core.model.MetadataDomain
import com.ivy.wallet.io.persistence.dao.MetadataDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

class MetadataRepository @Inject constructor(
    private val metadataDao: MetadataDao
) {
    suspend fun addMetadata(associatedId: UUID, property: MetadataProperties) {
        withContext(Dispatchers.IO) {
            metadataDao.save(
                dummyMetadataDomain()
                    .copy(
                        associatedId = associatedId,
                        property = property
                    )
                    .toEntity()
            )
        }
    }

    suspend fun deleteIndividualMetadata(associatedId: UUID, property: MetadataProperties) {
        withContext(Dispatchers.IO) {
            metadataDao.deleteMetadataProperty(associatedId, property)
        }
    }

    suspend fun findByAssociatedId(associatedId: UUID): List<MetadataDomain> {
        return withContext(Dispatchers.IO) {
            metadataDao.findByAssociatedId(associatedId).map { it.toDomain() }
        }
    }


    private fun dummyMetadataDomain() = MetadataDomain(associatedId = UUID.randomUUID())
}