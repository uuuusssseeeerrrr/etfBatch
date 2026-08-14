package com.ietf.etfbatch.config

import io.ktor.server.application.*
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger
import org.koin.plugin.module.dsl.*

@Module
@ComponentScan("com.ietf.etfbatch")
class AppModule

fun Application.koinConfig() {
    install(Koin) {
        slf4jLogger()
        modules(restClient)
        module<AppModule>()
    }
}