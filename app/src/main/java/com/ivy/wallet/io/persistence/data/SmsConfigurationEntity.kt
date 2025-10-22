package com.ivy.wallet.io.persistence.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ivy.wallet.domain.data.SmsConfigType
import com.ivy.wallet.domain.data.SmsMatchType
import java.util.UUID

@Entity(tableName = "sms_configuration")
data class SmsConfigurationEntity(
    @PrimaryKey
    val id: UUID = UUID.randomUUID(),
    val configType: SmsConfigType,
    val matchType: SmsMatchType
)