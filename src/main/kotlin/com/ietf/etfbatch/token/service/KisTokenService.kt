package com.ietf.etfbatch.token.service

import com.ietf.etfbatch.token.dto.KisTokenRequest
import com.ietf.etfbatch.token.dto.KisTokenResponse
import com.ietf.etfbatch.token.table.Token
import com.typesafe.config.ConfigFactory
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.flow.first
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class KisTokenService(val httpClient: HttpClient) {
    suspend fun getKisAccessToken(): String {
        val today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
        var accessToken = ""
        val tokenList = suspendTransaction {
            Token.selectAll()
                .where { Token.regDate eq today }
        }

        if (tokenList.empty()) {
            val config = ConfigFactory.load()

            val kisTokenRequest = KisTokenRequest(
                config.getString("custom.kis.key"),
                config.getString("custom.kis.secret")
            )

            val kisTokenResponse = tokenApiCall(kisTokenRequest)

            if (kisTokenResponse.accessToken.isNotEmpty()) {
                accessToken = kisTokenResponse.accessToken

                suspendTransaction {
                    Token.insert {
                        it[regDate] = today
                        it[token] = accessToken
                    }
                }
            }
        } else {
            accessToken = tokenList.first()[Token.token]
        }

        return accessToken
    }

    private suspend fun tokenApiCall(kisTokenRequest: KisTokenRequest): KisTokenResponse {
        val response: HttpResponse = httpClient.post("/oauth2/tokenP") {
            setBody(kisTokenRequest)
        }

        return response.body<KisTokenResponse>()
    }
}
