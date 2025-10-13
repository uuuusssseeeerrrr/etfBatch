package com.ietf.etfbatch.token.service

import com.ietf.etfbatch.token.dto.KisTokenRequest
import com.ietf.etfbatch.token.dto.KisTokenResponse
import com.ietf.etfbatch.token.model.Token
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.LocalDateTime

class KisTokenService(val httpClient: HttpClient) {
    fun getKisAccessToken(): String {
        val today = LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"))
        var accessToken = ""

        transaction {
            val tokenList = Token.selectAll()
                .where { Token.regDate eq today }

            if (tokenList.empty()) {
                val kisTokenRequest = KisTokenRequest(
                    System.getenv("custom.kis.key"),
                    System.getenv("custom.kis.secret")
                )

                val kisTokenResponse = runBlocking {
                    tokenApiCall(kisTokenRequest)
                }

                if (kisTokenResponse.accessToken.isNotEmpty()) {
                    accessToken = kisTokenResponse.accessToken

                    Token.insert {
                        it[regDate] = today
                        it[token] = accessToken
                    }
                }
            } else {
                accessToken = tokenList.first()[Token.token]
            }
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
