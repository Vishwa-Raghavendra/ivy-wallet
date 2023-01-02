package com.ivy.wallet.io.persistence.data

import androidx.compose.ui.graphics.toArgb
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ivy.wallet.core.model.Tag
import com.ivy.wallet.ui.theme.BlueLight
import java.util.*

@Entity(tableName = "tags")
data class TagEntity(
    @ColumnInfo(name = "tagName")
    val name: String,
    val color: Int = BlueLight.toArgb(),
    val icon: String? = null,
    val orderNum: Double = 0.0,

    @PrimaryKey
    val id: UUID = UUID.randomUUID()
){
    fun toDomain() : Tag {
        return Tag(name, color, icon, orderNum, id)
    }
}