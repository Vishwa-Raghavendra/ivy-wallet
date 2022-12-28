package com.ivy.wallet.core.data.repository

import com.ivy.wallet.core.utils.accountTransfersCategory
import com.ivy.wallet.core.utils.unSpecifiedCategory
import com.ivy.wallet.domain.data.core.Category
import com.ivy.wallet.io.persistence.dao.CategoryDao
import com.ivy.wallet.io.persistence.data.CategoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

class CategoriesRepository @Inject constructor(
    private val categoryDao: CategoryDao
) {
    suspend fun getAllCategories(): List<Category> {
        return withContext(Dispatchers.IO) {
            categoryDao.findAll().map(CategoryEntity::toDomain)
        }
    }

    suspend fun findById(id: UUID): Category? {
        return withContext(Dispatchers.IO) {
            when (id) {
                unSpecifiedCategory.id -> unSpecifiedCategory
                accountTransfersCategory.id -> accountTransfersCategory
                else -> categoryDao.findById(id)?.toDomain()
            }
        }
    }
}