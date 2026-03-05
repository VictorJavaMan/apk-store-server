package com.apkstore.server.database

import android.content.Context
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File

object DatabaseFactory {
    private var database: Database? = null

    fun init(context: Context) {
        val dbDir = File(context.filesDir, "database")
        if (!dbDir.exists()) {
            dbDir.mkdirs()
        }

        val dbPath = File(dbDir, "apkstore.db").absolutePath

        database = Database.connect(
            url = "jdbc:sqlite:$dbPath",
            driver = "org.sqlite.JDBC"
        )

        transaction(database!!) {
            SchemaUtils.create(ApkFiles)
        }
    }

    fun getDatabase(): Database {
        return database ?: throw IllegalStateException("Database not initialized. Call init() first.")
    }
}
