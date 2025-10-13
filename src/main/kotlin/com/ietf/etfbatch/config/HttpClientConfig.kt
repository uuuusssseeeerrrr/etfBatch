package com.ietf.etfbatch.config

import com.typesafe.config.ConfigFactory
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.config.HoconApplicationConfig
import kotlinx.serialization.json.Json
import org.koin.dsl.module

val restClient = module {
    val config = HoconApplicationConfig(ConfigFactory.load("application.yaml"))

    single {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json { prettyPrint = true })
            }

            defaultRequest {
                url("https://openapi.koreainvestment.com:9443")

                headers {
                    append(HttpHeaders.ContentType, ContentType.Application.Json)
                    append("appkey", config.property("custom.kis.key").getString())
                    append("appsecret", config.property("custom.kis.secret").getString())
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

