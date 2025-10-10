package com.ietf.etfbatch.token.service

import com.ietf.etfbatch.httpInf.KisInterface
import com.ietf.etfbatch.token.dto.KisTokenRequest
import com.ietf.etfbatch.token.model.Token
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class KisTokenService(val kisInterface: KisInterface) {
    @Value($$"${custom.kis.key}")
    lateinit var kisKey: String

    @Value($$"${custom.kis.secret}")
    lateinit var kisSecret: String

    fun getKisAccessToken(): String {
        val today = LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"))
        var accessToken = ""

        transaction {
            val tokenList = Token.selectAll()
                .where { Token.regDate eq today }

            if (tokenList.empty()) {
                val kisTokenRequest = KisTokenRequest(kisKey, kisSecret)
                val kisTokenResponse = kisInterface.tokenApiCall(kisTokenRequest)

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
}
