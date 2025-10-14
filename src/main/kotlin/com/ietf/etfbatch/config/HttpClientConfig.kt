package com.ietf.etfbatch.config

import com.ietf.etfbatch.token.service.KisTokenService
import com.typesafe.config.ConfigFactory
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import org.koin.dsl.module

class BearerTokenProvider {
    private var token: BearerTokens? = null
    private val mutex = Mutex()
    suspend fun loadToken(): BearerTokens? = mutex.withLock { token }
    suspend fun saveToken(newTokens: BearerTokens) = mutex.withLock { token = newTokens }
}

val restClient = module {
    val config = ConfigFactory.load()
    single { BearerTokenProvider() }
    single {
        val tokenProvider: BearerTokenProvider = get()
        val kisTokenService: KisTokenService = get()

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
                        append("appkey", config.getString("custom.kis.key"))
                        append("appsecret", config.getString("custom.kis.secret"))
                    }
                }
            }

            engine {
                endpoint {
                    maxConnectionsPerRoute = 4
                    keepAliveTime = 5000
                    connectTimeout = 5000
                    connectAttempts = 5
                }
            }

            install(Logging) {
                level = LogLevel.ALL
                logger = Logger.DEFAULT
            }

            install(Auth) {
                bearer {
                    loadTokens {
                        return@loadTokens tokenProvider.loadToken()
                    }

                    refreshTokens {
                        return@refreshTokens kisTokenService.getToken()
                    }

                    sendWithoutRequest { request ->
                        // 토큰을 필요로 하는 경로(tokenP는 제외)에 대해서만 동작하게 설정
                        !request.url.encodedPath.contains("/oauth2/tokenP")
                    }
                }
            }
        }
    }
}

