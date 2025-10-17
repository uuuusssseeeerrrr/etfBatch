package com.ietf.etfbatch.rate.service

import com.ietf.etfbatch.rate.dto.NaverRateResponse
import com.ietf.etfbatch.rate.table.Rate
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.math.BigDecimal
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class RateService {
    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                encodeDefaults = true
                prettyPrint = true
                ignoreUnknownKeys = true
            })
        }
    }

    @OptIn(ExperimentalTime::class)
    suspend fun getRate() {
        val usdRate = callApi("USD", "KRW", "1")
        val jpyRate = callApi("JPY", "KRW", "100")
        val sgdRate = callApi("SGD", "KRW", "1")
        val eurRate = callApi("EUR", "KRW", "1")

        transaction {
            Rate.insert {
                it[Rate.regDate] = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                it[Rate.usdRate] = BigDecimal(usdRate.country.last().value.replace(",", ""))
                it[Rate.jpyRate] = BigDecimal(jpyRate.country.last().value.replace(",", ""))
                it[Rate.sgdRate] = BigDecimal(sgdRate.country.last().value.replace(",", ""))
                it[Rate.eurRate] = BigDecimal(eurRate.country.last().value.replace(",", ""))
            }
        }
    }

    private suspend fun callApi(from: String, to: String, unit: String): NaverRateResponse {
        return httpClient.get(
            """https://m.search.naver.com/p/csearch/content/qapirender.nhn?key=calculator&pkid=141&q=%ED%99%98%EC%9C%A8&where=m&u1=shb&u6=standardUnit&u7=0
            |&u3=${from}
            |&u4=${to}
            |&u8=down
            |&u2=${unit}""".trimMargin()
        ).body<NaverRateResponse>()
    }
}