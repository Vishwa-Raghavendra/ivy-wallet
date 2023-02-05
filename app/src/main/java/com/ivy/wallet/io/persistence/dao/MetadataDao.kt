package com.ivy.wallet.io.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ivy.wallet.core.domain.io.MetadataProperties
import com.ivy.wallet.io.persistence.data.MetadataEntity
import java.util.*

@Dao
interface MetadataDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(value: MetadataEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(value: List<MetadataEntity>)

    @Query("SELECT * FROM metadata")
    suspend fun findAll(): List<MetadataEntity>

    @Query("SELECT * FROM metadata WHERE id = :id")
    suspend fun findById(id: UUID): MetadataEntity?

    @Query("SELECT * FROM metadata WHERE associatedId = :associatedId AND property =:property")
    suspend fun findByProperty(associatedId: UUID, property: MetadataProperties) : MetadataEntity?

    @Query("SELECT * FROM metadata WHERE associatedId = :associatedId")
    suspend fun findByAssociatedId(associatedId: UUID): List<MetadataEntity>

    @Query("DELETE FROM metadata")
    suspend fun deleteAll()

    @Query("DELETE FROM metadata WHERE associatedId = :associatedId AND property =:property")
    suspend fun deleteMetadataProperty(associatedId: UUID, property: MetadataProperties)

    @Query("DELETE FROM metadata WHERE associatedId = :associatedId")
    suspend fun deleteAssociationsByAssociateId(associatedId: UUID)
}