package com.ietf.etfbatch.config

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.koin.dsl.module

val restClient = module {
    single {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json {
                    encodeDefaults = true
                    prettyPrint = true
                    ignoreUnknownKeys = true
                })
            }

            engine {
                endpoint {
                    maxConnectionsPerRoute = 10
                    keepAliveTime = 30000
                    connectTimeout = 30000
                    connectAttempts = 5
                }
            }

            if (!System.getenv("DOCKER_ENV").equals("true", ignoreCase = true)) {
                install(Logging) {
                    logger = Logger.DEFAULT
                    level = LogLevel.ALL
                }
            }

            install(HttpTimeout) {
                requestTimeoutMillis = 100000
                connectTimeoutMillis = 60000
                socketTimeoutMillis = 60000
            }
        }
    }
}
