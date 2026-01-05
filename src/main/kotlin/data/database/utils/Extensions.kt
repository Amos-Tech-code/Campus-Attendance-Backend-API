package com.amos_tech_code.data.database.utils

import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

// Helper function for repository methods that don't need their own transaction boundary
suspend fun <T> exposedTransaction(statement: suspend () -> T): T =
    newSuspendedTransaction {
        statement()
    }