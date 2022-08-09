package com.ivy.wallet.io.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ivy.wallet.io.persistence.data.DocumentEntity
import java.util.*

@Dao
interface DocumentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(value: DocumentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(value: List<DocumentEntity>)

    @Query("SELECT * FROM documents where associatedId=:associatedId")
    suspend fun findByTransactionId(associatedId: UUID): List<DocumentEntity>

    @Query("SELECT * FROM documents")
    suspend fun findAll(): List<DocumentEntity>

    @Query("SELECT * FROM documents where id=:id")
    suspend fun findById(id: UUID): DocumentEntity

    @Query("DELETE FROM documents where id=:id")
    suspend fun deleteById(id: UUID)
}