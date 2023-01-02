package com.ivy.wallet.io.persistence.dao

import androidx.room.*
import com.ivy.wallet.io.persistence.data.TagEntity
import java.util.*

@Dao
interface TagDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(value: TagEntity)

    @Update
    suspend fun update(value: TagEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(value: List<TagEntity>)

    @Query("SELECT * FROM tags ORDER BY orderNum ASC")
    suspend fun findAll(): List<TagEntity>

    @Query("SELECT * FROM tags WHERE id = :id")
    suspend fun findById(id:UUID) : TagEntity?

    @Query("DELETE FROM tags WHERE id = :id")
    suspend fun deleteById(id: UUID)

    @Query("DELETE FROM tags")
    suspend fun deleteAll()

    @Query("SELECT * FROM tags JOIN tags_transaction ON tags.id = tags_transaction.tagId WHERE associatedId = :id")
    @RewriteQueriesToDropUnusedColumns
    suspend fun findTagsByTransactionId(id: UUID) : List<TagEntity>
}