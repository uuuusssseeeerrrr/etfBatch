package com.ietf.etfbatch.config

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
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

            defaultRequest {
                url("https://openapi.koreainvestment.com:9443")

                headers {
                    append(HttpHeaders.ContentType, "application/json; charset=utf-8")

                    if (!url.encodedPath.contains("/token")) {
                        append("appkey", VaultConfig.getVaultSecret("kis_key"))
                        append("appsecret", VaultConfig.getVaultSecret("kis_secret"))
                    }
                }
            }

            engine {
                endpoint {
                    maxConnectionsPerRoute = 10
                    keepAliveTime = 100000
                    connectTimeout = 100000
                    connectAttempts = 5
                }
            }

            if (!System.getenv("DOCKER_ENV").equals("true", ignoreCase = true)) {
                install(Logging) {
                    logger = Logger.DEFAULT
                    level = LogLevel.ALL
                }
            }
        }
    }
}

