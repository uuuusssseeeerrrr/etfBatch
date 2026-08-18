package com.ietf.etfbatch.token.service

import com.ietf.etfbatch.config.VaultConfig
import com.ietf.etfbatch.table.Token
import com.ietf.etfbatch.token.dto.KisTokenResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.koin.core.annotation.Single
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Single
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
        val appKey = VaultConfig.getVaultSecret("kis_key")
        val appSecret = VaultConfig.getVaultSecret("kis_secret")
        val paramMap = mapOf("grant_type" to "client_credentials", "appkey" to appKey, "appsecret" to appSecret)

        val response: HttpResponse = httpClient.post("https://openapi.koreainvestment.com:9443/oauth2/tokenP") {
            contentType(ContentType.Application.Json)
            setBody(paramMap)
        }

        return response.body<KisTokenResponse>()
    }
}
