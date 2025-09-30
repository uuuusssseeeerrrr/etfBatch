package com.ietf.etfbatch.token.service

import com.ietf.etfbatch.`interface`.KisInterface
import com.ietf.etfbatch.token.model.KisTokenRequest
import com.ietf.etfbatch.token.model.Token
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.springframework.beans.factory.annotation.Value
import java.time.LocalDateTime


class KisTokenService(val kisInterface: KisInterface) {
    @Value("\${kis.key}")
    lateinit var KIS_KEY: String

    @Value("\${kis.secret}")
    lateinit var KIS_SECRET: String

    fun getKisAccessToken(): String {
        val today = LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"))

        val tokenList = Token.selectAll()
            .where { Token.regDate eq today }

        if (tokenList.empty()) {
            val kisTokenRequest = KisTokenRequest()
            kisTokenRequest.appkey = KIS_KEY
            kisTokenRequest.appsecret = KIS_SECRET

            val kisTokenResponse = kisInterface.tokenApiCall(kisTokenRequest)
            if (kisTokenResponse.access_token.isNotEmpty()) {
                Token.insert {
                    it[regDate] = today
                    it[token] = kisTokenResponse.access_token
                }
            }

            return kisTokenResponse.access_token
        } else {
            return tokenList.first()[Token.token]
        }
    }
}
