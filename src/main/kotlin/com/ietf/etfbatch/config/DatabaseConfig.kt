package com.ietf.etfbatch.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.v1.jdbc.Database

object DataSourceFactory {
    fun init() {
        val isDocker = System.getenv("DOCKER_ENV")?.toBoolean() ?: false
        val dbUrlKey = if (isDocker) "db_url_docker" else "db_url"

        val config = HikariConfig().apply {
            jdbcUrl = "${VaultConfig.getVaultSecret(dbUrlKey)}?rewriteBatchedStatements=true"
            driverClassName = "org.mariadb.jdbc.Driver"
            username = "etf"
            password = VaultConfig.getVaultSecret("db_password")
            maximumPoolSize = 10
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        }

        Database.connect(HikariDataSource(config))
    }
}
