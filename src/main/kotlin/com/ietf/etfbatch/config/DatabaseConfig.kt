package com.ietf.etfbatch.config

import com.typesafe.config.ConfigFactory
import io.r2dbc.spi.IsolationLevel
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabaseConfig

object DataSourceFactory {
    fun init() {
        val config = ConfigFactory.load()

        R2dbcDatabase.connect(
            driver = "mariadb",
            url = config.getString("database.url"),
            user = config.getString("database.username"),
            password = config.getString("database.password"),
            databaseConfig = R2dbcDatabaseConfig {
                defaultMaxAttempts = 3
                defaultR2dbcIsolationLevel = IsolationLevel.READ_COMMITTED
            }
        )
    }
}
