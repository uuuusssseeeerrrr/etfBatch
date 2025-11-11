package com.ietf.etfbatch.stock.service

import com.ietf.etfbatch.stock.dto.KisPriceDetailOutput
import com.ietf.etfbatch.stock.dto.KisPriceDetailResponse
import com.ietf.etfbatch.stock.dto.StockObject
import com.ietf.etfbatch.stock.enum.PrdtTypeCd
import com.ietf.etfbatch.table.*
import com.ietf.etfbatch.token.service.KisTokenService
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.delay
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.slf4j.LoggerFactory
import java.time.LocalTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class KisStockPriceService(
    val httpClient: HttpClient,
    private val kisTokenService: KisTokenService
) {
    companion object {
        const val PRICE_DETAIL_TR_ID = "HHDFS76200200"
        const val MIN_INTERVAL = 112L
        private val logger = LoggerFactory.getLogger(KisStockPriceService::class.java)
    }

    /**
     * ETF 가격정보 가져오기
     */
    suspend fun getEtfPrice(market: String) {
        val now = Clock.System.now().toLocalDateTime(TimeZone.of("Asia/Seoul"))

        if (market == "TSE" && (now.hour !in 9..16)) {
            return
        }

        val targetEtfList = transaction {
            EtfList.select(EtfList.market, EtfList.stockCode)
                .where { EtfList.market eq market }
                .map { row ->
                    StockObject(row[EtfList.market], row[EtfList.stockCode])
                }.toList()
        }

        if (targetEtfList.isNotEmpty()) {
            val etfApiResultList = getPriceInfo(targetEtfList)

            transaction {
                EtfPriceHistory.batchInsert(etfApiResultList, shouldReturnGeneratedValues = false) { result ->
                    this[EtfPriceHistory.market] = market
                    this[EtfPriceHistory.stockCode] = result.stockCode ?: ""
                    this[EtfPriceHistory.open] = result.open
                    this[EtfPriceHistory.high] = result.high
                    this[EtfPriceHistory.low] = result.low
                    this[EtfPriceHistory.price] = result.last
                    this[EtfPriceHistory.lastDayPrice] = result.base
                    this[EtfPriceHistory.h52p] = result.h52p
                    this[EtfPriceHistory.l52p] = result.l52p
                    this[EtfPriceHistory.tXprc] = result.tXprc
                    this[EtfPriceHistory.tXdif] = result.tXdif
                    this[EtfPriceHistory.tXrat] = result.tXrat
                    this[EtfPriceHistory.tRate] = result.tRate
                    this[EtfPriceHistory.regDate] = now
                }
            }
        }

        transaction {
            PriceApiCallHistories.insert {
                it[PriceApiCallHistories.market] = market
                it[PriceApiCallHistories.successYn] = "Y"
                it[PriceApiCallHistories.esYn] = "E"
                it[PriceApiCallHistories.regDate] = now
            }
        }
    }

    /**
     * 종목별 가격정보 가져오기
     */
    suspend fun getStockPrice(market: String) {
        val now = Clock.System.now().toLocalDateTime(TimeZone.of("Asia/Seoul"))

        if (market == "TSE" && (now.hour !in 9..16)) {
            return
        }

        val targetStockList = transaction {
            StockList.select(StockList.market, StockList.stockCode)
                .where { StockList.market eq market }
                .map { row -> StockObject(row[StockList.market], row[StockList.stockCode]) }
                .toList()
        }

        if (targetStockList.isNotEmpty()) {
            val stockApiResultList = getPriceInfo(targetStockList)

            transaction {
                StockPriceHistory.batchInsert(stockApiResultList, shouldReturnGeneratedValues = false) { result ->
                    this[StockPriceHistory.market] = market
                    this[StockPriceHistory.stockCode] = result.stockCode ?: ""
                    this[StockPriceHistory.open] = result.open
                    this[StockPriceHistory.high] = result.high
                    this[StockPriceHistory.low] = result.low
                    this[StockPriceHistory.price] = result.last
                    this[StockPriceHistory.lastDayPrice] = result.base
                    this[StockPriceHistory.h52p] = result.h52p
                    this[StockPriceHistory.l52p] = result.l52p
                    this[StockPriceHistory.tXprc] = result.tXprc
                    this[StockPriceHistory.tXdif] = result.tXdif
                    this[StockPriceHistory.tXrat] = result.tXrat
                    this[StockPriceHistory.tRate] = result.tRate
                    this[StockPriceHistory.tomv] = result.tomv
                    this[StockPriceHistory.perx] = result.perx
                    this[StockPriceHistory.pbrx] = result.pbrx
                    this[StockPriceHistory.epsx] = result.epsx
                    this[StockPriceHistory.bpsx] = result.bpsx
                    this[StockPriceHistory.eIcod] = result.eIcod
                    this[StockPriceHistory.regDate] = now
                }
            }
        }

        transaction {
            PriceApiCallHistories.insert {
                it[PriceApiCallHistories.market] = market
                it[PriceApiCallHistories.successYn] = "Y"
                it[PriceApiCallHistories.esYn] = "S"
                it[PriceApiCallHistories.regDate] = now
            }
        }
    }

    private suspend fun getPriceInfo(targetList: List<StockObject>): List<KisPriceDetailOutput> {
        val apiResultList = mutableListOf<KisPriceDetailOutput>()
        val token = kisTokenService.getKisAccessToken()
        val type = setOf("NYS", "NAS", "AMS")
        val startBoundary = LocalTime.of(8, 59)
        val endBoundary = LocalTime.of(17, 59)
        val currentTime = LocalTime.now()

        for (stock in targetList) {
            val startTime = System.currentTimeMillis()
            var market = stock.market

            if (market in type &&
                (currentTime.isAfter(startBoundary) && currentTime.isBefore(endBoundary))
            ) {
                market = PrdtTypeCd.findDayMarketCodeByMarketCode(market)
            }

            val kisPriceHttpResponse: HttpResponse = httpClient.get("/uapi/overseas-price/v1/quotations/price-detail") {
                url {
                    parameters.append("EXCD", market)
                    parameters.append("SYMB", stock.stockCode)
                }

                header("authorization", "Bearer $token")
                header("tr_id", PRICE_DETAIL_TR_ID)
            }

            val kisPriceApiResult = kisPriceHttpResponse.body<KisPriceDetailResponse>()

            if (kisPriceApiResult.rtCd == "0" &&
                kisPriceApiResult.output != null
            ) {
                kisPriceApiResult.output.market = stock.market
                kisPriceApiResult.output.stockCode = stock.stockCode
                apiResultList.add(kisPriceApiResult.output)
            }

            val elapsedTimeMs = System.currentTimeMillis() - startTime
            val delayTimeMs = MIN_INTERVAL - elapsedTimeMs
            if (delayTimeMs > 0) {
                // 목표 간격보다 빨리 끝났다면, 남은 시간만큼 대기
                delay(delayTimeMs)
                logger.debug("stockApiCall -> ${delayTimeMs}ms 지연 후 다음 호출.")
            }
        }

        return apiResultList
    }
}
