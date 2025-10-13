package com.ietf.etfbatch.config

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.koin.dsl.module

val restClient = module {
    single {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json { prettyPrint = true })
            }

            defaultRequest {
                url("https://openapi.koreainvestment.com:9443")

                headers {
                    append(HttpHeaders.ContentType, ContentType.Application.Json)
                    append("appkey", System.getenv("custom.kis.key"))
                    append("appsecret", System.getenv("custom.kis.secret"))
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
        }
    }
}

