package com.ietf.etfbatch.rate.service

import com.ietf.etfbatch.config.VaultConfig
import com.ietf.etfbatch.rate.dto.WiseRateResponse
import com.ietf.etfbatch.table.Rate
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class RateService(val client: HttpClient) {
    @OptIn(ExperimentalTime::class)
    suspend fun getRate() {
        val usdRate = callApi("USD", "KRW")
        val jpyRate = callApi("JPY", "KRW")
        val sgdRate = callApi("SGD", "KRW")
        val eurRate = callApi("EUR", "KRW")

        transaction {
            Rate.insert {
                it[Rate.regDate] = Clock.System.now().toLocalDateTime(TimeZone.of("Asia/Seoul"))
                it[Rate.usdRate] = usdRate.rate.toBigDecimal()
                it[Rate.jpyRate] = jpyRate.rate.toBigDecimal().multiply(100.toBigDecimal())
                it[Rate.sgdRate] = sgdRate.rate.toBigDecimal()
                it[Rate.eurRate] = eurRate.rate.toBigDecimal()
            }
        }
    }

    private suspend fun callApi(from: String, to: String): WiseRateResponse {
        return client.get {
            url("https://api.transferwise.com/v1/rates?source=${from}&target=${to}")
            header("Authorization", "Bearer ${VaultConfig.getVaultSecret("wise_key")}")
        }.body<List<WiseRateResponse>>().first()
    }
}