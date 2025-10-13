package com.ietf.etfbatch.stock.service

import com.ietf.etfbatch.stock.dto.KisPriceDetailOutput
import com.ietf.etfbatch.stock.dto.KisPriceDetailResponse
import com.ietf.etfbatch.stock.dto.StockObject
import com.ietf.etfbatch.stock.table.EtfList
import com.ietf.etfbatch.stock.table.EtfPriceHistory
import com.ietf.etfbatch.stock.table.StockList
import com.ietf.etfbatch.stock.table.StockPriceHistory
import com.ietf.etfbatch.token.service.KisTokenService
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.get
import io.ktor.client.statement.*
import io.ktor.http.headers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.r2dbc.batchInsert
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.slf4j.LoggerFactory
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class KisStockService(val httpClient: HttpClient, val kisTokenService: KisTokenService) {
    companion object {
        const val PRICE_DETAIL_TR_ID = "HHDFS76200200"
        const val MIN_INTERVAL = 112L
        private val logger = LoggerFactory.getLogger(KisStockService::class.java)
    }

    /**
     * ETF 가격정보 가져오기
     */
    suspend fun getEtfPrice() {
        val targetEtfList = suspendTransaction {
            EtfList.select(EtfList.market, EtfList.stockCode).map { row ->
                StockObject(row[EtfList.market], row[EtfList.stockCode])
            }.toList()
        }

        if (targetEtfList.isNotEmpty()) {
            val etfApiResultList = getPriceInfo(targetEtfList)

            suspendTransaction {
                EtfPriceHistory.batchInsert(etfApiResultList, shouldReturnGeneratedValues = false) { result ->
                    this[EtfPriceHistory.market] = result.market ?: "TSE"
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
                    this[EtfPriceHistory.regDate] =
                        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                }
            }
        }
    }

    /**
     * 종목별 가격정보 가져오기
     */
    suspend fun getStockPrice() {
        val targetStockList = suspendTransaction {
            StockList.select(StockList.market, StockList.stockCode)
                .limit(10)
                .map { row -> StockObject(row[StockList.market], row[StockList.stockCode]) }
                .toList()
        }

        if (targetStockList.isNotEmpty()) {
            val stockApiResultList = getPriceInfo(targetStockList)

            suspendTransaction {
                StockPriceHistory.batchInsert(stockApiResultList, shouldReturnGeneratedValues = false) { result ->
                    this[StockPriceHistory.market] = result.market ?: "TSE"
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
                    this[StockPriceHistory.regDate] =
                        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                }
            }
        }
    }

    private suspend fun getPriceInfo(targetList: List<StockObject>): List<KisPriceDetailOutput> {
        val token = suspendTransaction {
            kisTokenService.getKisAccessToken()
        }
        val apiResultList = mutableListOf<KisPriceDetailOutput>()

        for (stock in targetList) {
            val startTime = System.currentTimeMillis()

            val kisPriceHttpResponse: HttpResponse = httpClient.get("/uapi/overseas-price/v1/quotations/price-detail") {
                headers {
                    set("tr_id", PRICE_DETAIL_TR_ID)
                    set("Authorization", "Bearer $token")
                }

                url {
                    parameters.append("market", stock.market)
                    parameters.append("stock_code", stock.stockCode)
                }
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
                Thread.sleep(delayTimeMs)
                logger.debug("stockApiCall -> ${delayTimeMs}ms 지연 후 다음 호출.")
            }
        }

        return apiResultList
    }
}
