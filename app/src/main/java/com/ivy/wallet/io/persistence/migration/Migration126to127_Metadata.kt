package com.ivy.wallet.io.persistence.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration126to127_Metadata : Migration(126, 127) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("CREATE TABLE IF NOT EXISTS `metadata` (`id` TEXT NOT NULL,`associatedId` TEXT NOT NULL, `property` TEXT NOT NULL DEFAULT 'NONE', `value` TEXT NOT NULL DEFAULT '', PRIMARY KEY(`id`))")
    }
}