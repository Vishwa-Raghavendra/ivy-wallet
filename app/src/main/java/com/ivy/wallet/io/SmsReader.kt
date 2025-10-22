package com.ivy.wallet.io

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.provider.Telephony
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmsReader @Inject constructor(@ApplicationContext private val context: Context) {

    fun hasSmsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun readSmsMessages(startDate: Date? = null): List<SmsMessage> {
        if (!hasSmsPermission()) {
            return emptyList()
        }

        val messages = mutableListOf<SmsMessage>()
        val selection = if (startDate != null) "${Telephony.Sms.DATE} >= ?" else null
        val selectionArgs = if (startDate != null) arrayOf(startDate.time.toString()) else null

        val cursor: Cursor? = context.contentResolver.query(
            Telephony.Sms.CONTENT_URI, // The content URI of the SMS provider
            null, // The columns to return for each row
            null, // A filter to apply to the rows in the form of a WHERE clause
            null, // The arguments to substitution for the filter
            null // The order in which the rows should be returned
        )

        cursor?.use {
            val indexBody = it.getColumnIndex(Telephony.Sms.BODY)
            val indexAddress = it.getColumnIndex(Telephony.Sms.ADDRESS)
            val indexDate = it.getColumnIndex(Telephony.Sms.DATE)

            if (indexBody != -1 && indexAddress != -1 && indexDate != -1) {
                while (it.moveToNext()) {
                    val address = it.getString(indexAddress)
                    val body = it.getString(indexBody)
                    val date = it.getLong(indexDate)
                    messages.add(SmsMessage(address, body, date))
                }
            }
        }

        return messages
    }
}

data class SmsMessage(val address: String, val body: String, val date: Long)
