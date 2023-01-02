package com.ivy.wallet.core.model

import androidx.compose.ui.graphics.toArgb
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ivy.wallet.io.persistence.data.TagEntity
import com.ivy.wallet.ui.theme.BlueLight
import java.util.*

data class Tag(
    val name: String,
    val color: Int = BlueLight.toArgb(),
    val icon: String? = null,
    val orderNum: Double = 0.0,
    val id: UUID = UUID.randomUUID()
) {
    fun toEntity() : TagEntity {
        return TagEntity(name, color, icon, orderNum, id)
    }
}