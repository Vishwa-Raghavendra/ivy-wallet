package com.ivy.wallet.core.data.repository

import com.ivy.wallet.core.model.Tag
import com.ivy.wallet.core.model.TagTransaction
import com.ivy.wallet.io.persistence.dao.TagDao
import com.ivy.wallet.io.persistence.dao.TagTransactionDao
import com.ivy.wallet.io.persistence.data.TagEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject

class TagsRepository @Inject constructor(
    private val tagDao: TagDao,
    private val tagTransactionDao: TagTransactionDao
) {

    suspend fun getAllTags(): List<Tag> {
        return withContext(Dispatchers.IO) {
            tagDao.findAll().map(TagEntity::toDomain)
        }
    }

    suspend fun findTagById(id: UUID): Tag? {
        return withContext(Dispatchers.IO) {
            tagDao.findById(id)?.toDomain()
        }
    }

    suspend fun findTagsByTransactionId(id: UUID): List<Tag> {
        return withContext(Dispatchers.IO) {
            tagDao.findTagsByTransactionId(id).map(TagEntity::toDomain)
        }
    }

    suspend fun saveTag(tag: Tag) {
        withContext(Dispatchers.IO) {
            tagDao.save(tag.toEntity())
        }
    }

    suspend fun updateTag(tag: Tag) {
        withContext(Dispatchers.IO) {
            tagDao.update(tag.toEntity())
        }
    }

    suspend fun deleteTag(tag: Tag) {
        withContext(Dispatchers.IO) {
            tagDao.deleteById(tag.id)
            tagTransactionDao.deleteAssociationsByTagId(tag.id)
        }
    }

    suspend fun saveAndAssociateTag(transactionId: UUID, tag: Tag) {
        withContext(Dispatchers.IO) {
            saveTag(tag)
            tagTransactionDao.save(TagTransaction(tag.id, transactionId).toEntity())
        }
    }

    suspend fun associateTag(transactionId: UUID, tag: Tag) {
        withContext(Dispatchers.IO) {
            tagTransactionDao.save(TagTransaction(tag.id, transactionId).toEntity())
        }
    }

    suspend fun deleteAssociation(transactionId: UUID, tag: Tag) {
        withContext(Dispatchers.IO) {
            tagTransactionDao.deleteId(tag.id, transactionId)
        }
    }

    suspend fun deleteAllAssociations(transactionId: UUID) {
        withContext(Dispatchers.IO) {
            tagTransactionDao.deleteAssociationsByAssociateId(transactionId)
        }
    }
}