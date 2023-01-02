package com.ivy.wallet.io.persistence.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration125to126_Tags : Migration(125, 126) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("CREATE TABLE IF NOT EXISTS `tags` (`tagName` TEXT NOT NULL, `color` INTEGER NOT NULL, `icon` TEXT, `orderNum` REAL NOT NULL, `id` TEXT NOT NULL, PRIMARY KEY(`id`))")
        database.execSQL("CREATE TABLE IF NOT EXISTS `tags_transaction` (`tagId` TEXT NOT NULL,`associatedId` TEXT NOT NULL, PRIMARY KEY(`tagId`,`associatedId`))")
    }
}