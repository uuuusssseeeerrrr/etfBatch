package com.ietf.etfbatch.config

import org.jetbrains.exposed.v1.core.DatabaseConfig
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager

object DataSourceFactory {
    fun init() {
        val isDocker = System.getenv("DOCKER_ENV")?.toBoolean() ?: false
        val dbUrlKey = if (isDocker) "DB_URL_DOCKER" else "DB_URL"

        val db = Database.connect(
            driver = "org.mariadb.jdbc.Driver",
            url = "${VaultConfig.getVaultSecret(dbUrlKey)}?rewriteBatchedStatements=true",
            user = "etf",
            password = VaultConfig.getVaultSecret("db_password"),
            databaseConfig = DatabaseConfig {
                defaultMaxAttempts = 3
            }
        )

        TransactionManager.defaultDatabase = db
    }
}
