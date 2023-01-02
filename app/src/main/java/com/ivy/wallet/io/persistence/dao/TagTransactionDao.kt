package com.ivy.wallet.io.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ivy.wallet.io.persistence.data.TagTransactionEntity
import java.util.*

@Dao
interface TagTransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(value: TagTransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(value: List<TagTransactionEntity>)

    @Query("SELECT * FROM tags_transaction")
    suspend fun findAll(): List<TagTransactionEntity>

    @Query("SELECT * FROM tags_transaction WHERE tagId = :id")
    suspend fun findById(id: UUID): TagTransactionEntity?

    @Query("DELETE FROM tags_transaction")
    suspend fun deleteAll()

    @Query("DELETE FROM tags_transaction WHERE tagId = :tagId AND associatedId = :associatedId")
    suspend fun deleteId(tagId: UUID, associatedId: UUID)

    @Query("DELETE FROM tags_transaction WHERE tagId = :tagId")
    suspend fun deleteAssociationsByTagId(tagId: UUID)

    @Query("DELETE FROM tags_transaction WHERE associatedId = :associatedId")
    suspend fun deleteAssociationsByAssociateId(associatedId: UUID)
}