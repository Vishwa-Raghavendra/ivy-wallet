package com.ivy.wallet.domain.data

import java.util.UUID


data class SmsConfiguration(
    val id: UUID,
    val configType: SmsConfigType,
    val matchType: SmsMatchType
)