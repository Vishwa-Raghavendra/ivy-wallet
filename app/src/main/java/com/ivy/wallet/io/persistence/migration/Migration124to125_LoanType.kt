package com.ivy.wallet.io.persistence.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration124to125_LoanType : Migration(124,125) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE loan_records ADD COLUMN loanRecordType TEXT NOT NULL DEFAULT 'DEFAULT'")
    }
}