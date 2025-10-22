package com.ietf.etfbatch.token.service

import com.ietf.etfbatch.token.dto.KisTokenRequest
import com.ietf.etfbatch.token.dto.KisTokenResponse
import com.ietf.etfbatch.table.Token
import com.typesafe.config.ConfigFactory
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class KisTokenService(val httpClient: HttpClient) {
    suspend fun getKisAccessToken(): String {
        val today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
        var accessToken = ""
        val tokenList = transaction {
            Token.selectAll()
                .where { Token.regDate eq today }
                .map { it[Token.token] }
                .toList()
        }

        if (tokenList.isEmpty()) {
            val kisTokenResponse = tokenApiCall()

            if (kisTokenResponse.accessToken != null) {
                accessToken = kisTokenResponse.accessToken
                transaction {
                    Token.insert {
                        it[regDate] = today
                        it[token] = accessToken
                    }
                }
            }
        } else {
            accessToken = tokenList.last()
        }

        return accessToken
    }

    private suspend fun tokenApiCall(): KisTokenResponse {
        val config = ConfigFactory.load()

        val response: HttpResponse = httpClient.post("/oauth2/tokenP") {
            setBody(
                KisTokenRequest(
                    appKey = config.getString("custom.kis.key"),
                    appSecret = config.getString("custom.kis.secret")
                )
            )

            Json {
                encodeDefaults = true
                prettyPrint = true
            }
        }

        return response.body<KisTokenResponse>()
    }
}
