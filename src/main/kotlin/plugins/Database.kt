package com.amos_tech_code.plugins

import com.amos_tech_code.data.database.DatabaseFactory
import com.amos_tech_code.data.database.MigrationManager
import com.amos_tech_code.data.database.SeedManager
import io.ktor.server.application.*

fun Application.configureDatabase() {

    try {

        DatabaseFactory.connect()

        MigrationManager.migrate()

        DatabaseFactory.createSchema()

        SeedManager.seed()

        log.info("✅ Database initialized successfully")

    } catch (e: Exception) {

        log.error("❌ Database initialization failed", e)

        /**
         * CRITICAL:
         * Stop server startup completely.
         * Running without DB = corrupted backend.
         */
        throw e
    }
}