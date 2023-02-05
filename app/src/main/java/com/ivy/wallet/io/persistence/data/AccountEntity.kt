package com.ivy.wallet.io.persistence.data

import androidx.compose.ui.graphics.toArgb
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ivy.wallet.core.domain.io.MetadataProperties
import com.ivy.wallet.core.model.MetadataDomain
import com.ivy.wallet.domain.data.core.Account
import com.ivy.wallet.ui.theme.Green
import java.util.*

@Entity(tableName = "accounts")
data class AccountEntity(
    val name: String,
    val currency: String? = null,
    val color: Int = Green.toArgb(),
    val icon: String? = null,
    val orderNum: Double = 0.0,
    val includeInBalance: Boolean = true,

    val isSynced: Boolean = false,
    val isDeleted: Boolean = false,

    @PrimaryKey
    val id: UUID = UUID.randomUUID()
) {
    fun toDomain(
        baseCurrency: String = "",
        metadataProperties: List<MetadataDomain> = emptyList()
    ): Account = Account(
        name = name,
        currency = currency,
        currencyOrBaseCurrency = currency ?: baseCurrency,
        color = color,
        icon = icon,
        orderNum = orderNum,
        includeInBalance = includeInBalance,
        isSynced = isSynced,
        isDeleted = isDeleted,
        id = id,
        metadata = metadataProperties,
        metadataPropertyValues = metadataProperties.map { it.property }.toSet()
    )
}