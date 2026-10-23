package com.ietf.etfbatch.config

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.koin.dsl.module

private val SECRET_BODY_REGEX = Regex("\"(appkey|appsecret|access_token)\"\\s*:\\s*\"[^\"]*\"")

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
                    // 토큰 발급 요청/응답 바디의 appkey, appsecret, access_token 값을 *** 로 마스킹
                    logger = object : Logger {
                        override fun log(message: String) {
                            Logger.DEFAULT.log(message.replace(SECRET_BODY_REGEX, "\"$1\": \"***\""))
                        }
                    }
                    level = LogLevel.ALL
                    sanitizeHeader { header -> header.lowercase() in listOf("appkey", "appsecret", "authorization", "cookie", "set-cookie") }
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
