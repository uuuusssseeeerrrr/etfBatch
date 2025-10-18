package com.ietf.etfbatch.rate.service

import com.ietf.etfbatch.rate.dto.WiseRateResponse
import com.ietf.etfbatch.rate.table.Rate
import com.typesafe.config.ConfigFactory
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class RateService {
    private val httpClient = HttpClient(CIO) {
        val config = ConfigFactory.load()

        install(ContentNegotiation) {
            json(Json {
                encodeDefaults = true
                prettyPrint = true
                ignoreUnknownKeys = true
            })
        }

        defaultRequest {
            header("Authorization", "Bearer ${config.getString("custom.wise.key")}")
        }
    }

    @OptIn(ExperimentalTime::class)
    suspend fun getRate() {
        val usdRate = callApi("USD", "KRW")
        val jpyRate = callApi("JPY", "KRW")
        val sgdRate = callApi("SGD", "KRW")
        val eurRate = callApi("EUR", "KRW")

        transaction {
            Rate.insert {
                it[Rate.regDate] = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                it[Rate.usdRate] = usdRate.rate.toBigDecimal()
                it[Rate.jpyRate] = jpyRate.rate.toBigDecimal().multiply(100.toBigDecimal())
                it[Rate.sgdRate] = sgdRate.rate.toBigDecimal()
                it[Rate.eurRate] = eurRate.rate.toBigDecimal()
            }
        }
    }

    private suspend fun callApi(from: String, to: String): WiseRateResponse {
        return httpClient.get(
            """https://api.transferwise.com/v1/rates?source=${from}&target=${to}"""
        ).body<List<WiseRateResponse>>().first()
    }
}