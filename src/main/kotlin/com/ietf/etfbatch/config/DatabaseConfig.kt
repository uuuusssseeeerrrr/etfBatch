package com.ietf.etfbatch.config

import io.r2dbc.spi.IsolationLevel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.newFixedThreadPoolContext
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabaseConfig
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction

object dataSourceFactory {
    fun init() {
        val database = R2dbcDatabase.connect(
            driver = "mariadb",
            url = System.getenv("ktor.datasource.url"),
            user = System.getenv("ktor.datasource.username"),
            password = System.getenv("ktor.datasource.password"),
            databaseConfig = R2dbcDatabaseConfig {
                defaultMaxAttempts = 3
                defaultR2dbcIsolationLevel = IsolationLevel.READ_COMMITTED
            }
        )
    }
}

suspend fun <T> dbQuery(block: suspend () -> T): T =
    suspendTransaction { block() }