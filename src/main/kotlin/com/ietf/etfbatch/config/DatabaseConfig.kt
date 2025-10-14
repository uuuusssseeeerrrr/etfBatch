package com.ietf.etfbatch.config

import com.typesafe.config.ConfigFactory
import org.jetbrains.exposed.v1.core.DatabaseConfig
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager

object DataSourceFactory {
    fun init() {
        val config = ConfigFactory.load()

        val db = Database.connect(
            driver = "org.mariadb.jdbc.Driver",
            url = config.getString("database.url"),
            user = config.getString("database.username"),
            password = config.getString("database.password"),
            databaseConfig = DatabaseConfig {
                defaultMaxAttempts = 3
            }
        )

        TransactionManager.defaultDatabase = db
    }
}
