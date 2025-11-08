package com.ietf.etfbatch.stock.service

import com.ietf.etfbatch.stock.dto.KisInfoOutput
import com.ietf.etfbatch.stock.dto.KisInfoResponse
import com.ietf.etfbatch.stock.dto.StockObject
import com.ietf.etfbatch.stock.enum.PrdtTypeCd
import com.ietf.etfbatch.table.EtfList
import com.ietf.etfbatch.table.StockList
import com.ietf.etfbatch.token.service.KisTokenService
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.slf4j.LoggerFactory
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class KisStockInfoService(
    val httpClient: HttpClient,
    private val kisTokenService: KisTokenService
) {
    companion object {
        const val SEARCH_INFO_TR_ID = "CTPF1702R"
        const val MIN_INTERVAL = 112L
        private val logger = LoggerFactory.getLogger(KisStockInfoService::class.java)
    }

    suspend fun kisInfo() {
        getEtfInfo()
        getStockInfo()
    }

    /**
     * ETF 정보 가져오기
     */
    suspend fun getEtfInfo() {
        val newEtfList = transaction {
            EtfList.select(EtfList.market, EtfList.stockCode)
                .where { (EtfList.stdPdno.isNull()) or (EtfList.stdPdno.trim().eq("")) }
                .map { row ->
                    StockObject(row[EtfList.market], row[EtfList.stockCode])
                }.toList()
        }

        if (newEtfList.isNotEmpty()) {
            val etfApiResultList = getInfo(newEtfList)

            etfApiResultList.forEach { apiResult ->
                transaction {
                    EtfList.update({
                        EtfList.market.eq(
                            apiResult.market
                        ) and EtfList.stockCode.eq(apiResult.stockCode)
                    }) {
                        it[EtfList.tradingLot] = apiResult.buyUnitQty
                        it[EtfList.stdPdno] = apiResult.stdPdno
                        it[EtfList.isinCode] = apiResult.isinCd
                        it[EtfList.modDate] =
                            Clock.System.now().toLocalDateTime(TimeZone.of("Asia/Seoul"))
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    suspend fun getStockInfo() {
        val newStockList = transaction {
            StockList.select(StockList.market, StockList.stockCode)
                .where { (StockList.stdPdno.isNull()) or (StockList.stdPdno.trim().eq("")) }
                .map { row ->
                    StockObject(row[StockList.market], row[StockList.stockCode])
                }.toList()
        }

        if (newStockList.isNotEmpty()) {
            val today = Clock.System.now().toLocalDateTime(TimeZone.of("Asia/Seoul"))
            val apiResultList = getInfo(newStockList)

            coroutineScope {
                apiResultList.forEach { apiResult ->
                    async(Dispatchers.IO) {
                        transaction {
                            StockList.update({
                                StockList.market.eq(apiResult.market) and
                                        StockList.stockCode.eq(apiResult.stockCode)
                            }) {
                                it[StockList.trCrcyCd] = apiResult.trCrcyCd
                                it[StockList.buyUnitQty] = apiResult.buyUnitQty
                                it[StockList.stdPdno] = apiResult.stdPdno
                                it[StockList.prdtName] = if (apiResult.prdtName.contains("]")) {
                                    apiResult.prdtName.split(']')[1]
                                } else {
                                    apiResult.prdtName
                                }

                                it[StockList.isinCode] = apiResult.isinCd
                                it[StockList.stockName] = if (apiResult.prdtEngName.contains("]")) {
                                    apiResult.prdtEngName.split(']')[1]
                                } else {
                                    apiResult.prdtEngName
                                }
                                it[StockList.modDate] = today
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun getInfo(targetList: List<StockObject>): List<KisInfoOutput> {
        val apiResultList = mutableListOf<KisInfoOutput>()
        val token = kisTokenService.getKisAccessToken()

        for (stock in targetList) {
            val startTime = System.currentTimeMillis()
            val kisApiResult = httpClient.get("/uapi/overseas-price/v1/quotations/search-info") {
                header("authorization", "Bearer $token")
                header("tr_id", SEARCH_INFO_TR_ID)
                header("custtype", "P")

                url {
                    parameters.append("PRDT_TYPE_CD", PrdtTypeCd.findInfoCodeByMarketCode(stock.market).toString())
                    parameters.append("PDNO", stock.stockCode)
                }
            }.body<KisInfoResponse>()

            if (kisApiResult.rtCd == "0" &&
                kisApiResult.output != null
            ) {
                kisApiResult.output.market = stock.market
                kisApiResult.output.stockCode = stock.stockCode
                apiResultList.add(kisApiResult.output)
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
