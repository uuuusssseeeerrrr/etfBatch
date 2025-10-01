package com.ietf.etfbatch.config

import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.springframework.context.annotation.Configuration
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import java.sql.Connection
import javax.sql.DataSource

@Configuration
class DatabaseConfig(private val dataSource: DataSource) {
    @EventListener(ContextRefreshedEvent::class)
    fun initExposedDatabase() {
        Database.connect(dataSource)

        TransactionManager.defaultDatabase = Database.connect(dataSource)
        TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_READ_COMMITTED
    }
}