package com.ivy.wallet.io.persistence.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration123to124_DocumentsTable : Migration(123,124) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("CREATE TABLE IF NOT EXISTS `documents` (`transactionId` TEXT NOT NULL, `filePath` TEXT NOT NULL, `id` TEXT NOT NULL, PRIMARY KEY(`id`))")
    }
}