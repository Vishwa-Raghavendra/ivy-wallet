package com.ivy.wallet.io.persistence.dao

import androidx.room.Dao
import androidx.room.Query
import com.ivy.wallet.domain.data.SmsConfigType
import com.ivy.wallet.io.persistence.data.SmsConfigurationEntity

@Dao
interface SmsConfigurationDao {
    @Query("SELECT * FROM sms_configuration WHERE id = :id")
    suspend fun getById(id: Int): SmsConfigurationEntity?

    @Query("SELECT * FROM sms_configuration WHERE configType = :configType")
    suspend fun getByConfigType(configType: SmsConfigType): List<SmsConfigurationEntity>

    @Query("SELECT * FROM sms_configuration")
    suspend fun getAll(): List<SmsConfigurationEntity>
}