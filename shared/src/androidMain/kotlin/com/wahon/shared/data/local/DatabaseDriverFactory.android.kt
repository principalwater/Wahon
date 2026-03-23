package com.wahon.shared.data.local

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

actual class DatabaseDriverFactory(
    private val context: Context,
) {
    actual fun createDriver(): SqlDriver = AndroidSqliteDriver(
        schema = WahonDatabase.Schema,
        context = context,
        name = "wahon.db",
        callback = object : AndroidSqliteDriver.Callback(WahonDatabase.Schema) {
            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                setPragma(db, "foreign_keys = ON")
                setPragma(db, "journal_mode = WAL")
                setPragma(db, "synchronous = NORMAL")
            }

            private fun setPragma(db: SupportSQLiteDatabase, pragma: String) {
                db.query("PRAGMA $pragma").use { cursor ->
                    cursor.moveToFirst()
                }
            }
        },
    )
}
