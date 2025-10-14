package com.ietf.etfbatch.token.service

import com.ietf.etfbatch.config.BearerTokenProvider
import com.ietf.etfbatch.token.dto.KisTokenRequest
import com.ietf.etfbatch.token.dto.KisTokenResponse
import com.typesafe.config.ConfigFactory
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

class KisTokenService(private val tokenProvider: BearerTokenProvider) {
    suspend fun getToken(): BearerTokens {
        if (tokenProvider.loadToken() == null) {
            val config = ConfigFactory.load()
            val tokenRefreshClient = HttpClient(CIO) {
                install(ContentNegotiation) {
                    json(Json {
                        encodeDefaults = true
                        prettyPrint = true
                    })
                }

                defaultRequest {
                    header(HttpHeaders.ContentType, "application/json; charset=utf-8")
                }
            }

            val response: HttpResponse =
                tokenRefreshClient.post("https://openapi.koreainvestment.com:9443/oauth2/tokenP") {
                    setBody(
                        KisTokenRequest(
                            appKey = config.getString("custom.kis.key"),
                            appSecret = config.getString("custom.kis.secret")
                        )
                    )
                }

            val tokenResponse = response.body<KisTokenResponse>()
            val newTokens = BearerTokens(tokenResponse.accessToken ?: "", tokenResponse.accessToken ?: "")
            tokenProvider.saveToken(newTokens)

            return newTokens
        } else {
            return tokenProvider.loadToken()!!
        }
    }
}
