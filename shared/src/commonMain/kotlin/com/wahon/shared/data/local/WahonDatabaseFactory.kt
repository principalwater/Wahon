package com.wahon.shared.data.local

import app.cash.sqldelight.db.SqlDriver

class WahonDatabaseFactory(
    private val driverFactory: DatabaseDriverFactory,
) {
    fun create(): WahonDatabase {
        val driver = driverFactory.createDriver()
        configureDriver(driver)
        val database = WahonDatabase(driver)
        repairOrphans(database)
        return database
    }

    private fun configureDriver(driver: SqlDriver) {
        runCatching {
            driver.execute(
                identifier = null,
                sql = "PRAGMA foreign_keys = ON",
                parameters = 0,
                binders = null,
            )
        }
    }

    private fun repairOrphans(database: WahonDatabase) {
        runCatching { database.chapterQueries.deleteOrphanedChapters() }
        runCatching { database.historyQueries.deleteOrphanedHistory() }
    }
}
