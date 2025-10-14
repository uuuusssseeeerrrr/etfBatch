package com.ietf.etfbatch.stock.service

import com.ietf.etfbatch.stock.dto.KisInfoOutput
import com.ietf.etfbatch.stock.dto.KisInfoRequest
import com.ietf.etfbatch.stock.dto.KisInfoResponse
import com.ietf.etfbatch.stock.dto.StockObject
import com.ietf.etfbatch.stock.table.EtfList
import com.ietf.etfbatch.stock.table.StockList
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.headers
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
class KisInfoService(val httpClient: HttpClient) {
    companion object {
        const val SEARCH_INFO_TR_ID = "CTPF1702R"
        const val MIN_INTERVAL = 112L
        private val logger = LoggerFactory.getLogger(KisInfoService::class.java)
    }

    suspend fun kisInfo() {
        getEtfInfo()
        getStockInfo()
    }

    /**
     * ETF 정보 가져오기
     */
    private suspend fun getEtfInfo() {
        val reqEtfInfoList = transaction {
            EtfList.select(EtfList.market, EtfList.stockCode)
                .where { (EtfList.stdPdno.isNull()) or (EtfList.stdPdno.trim().eq("")) }
                .map { row ->
                    StockObject(row[EtfList.market], row[EtfList.stockCode])
                }.toList()
        }

        if (reqEtfInfoList.isNotEmpty()) {
            val etfApiResultList = getInfo(reqEtfInfoList)

            etfApiResultList.filter { infoOutput ->
                !infoOutput.market.isNullOrEmpty()
                        && !infoOutput.stockCode.isNullOrEmpty()
            }
                .forEach { apiResult ->
                    transaction {
                        EtfList.update({
                            EtfList.market.eq(
                                apiResult.market!!
                            ) and EtfList.stockCode.eq(apiResult.stockCode!!)
                        }) {
                            it[EtfList.tradingLot] = apiResult.buyUnitQty
                            it[EtfList.stdPdno] = apiResult.stdPdno
                            it[EtfList.modDate] =
                                Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                        }
                    }
                }
        }
    }

    @OptIn(ExperimentalTime::class)
    private suspend fun getStockInfo() {
        val reqStockInfoList = transaction {
            StockList.select(StockList.market, StockList.stockCode)
                .where { (StockList.stdPdno.isNull()) or (StockList.stdPdno.trim().eq("")) }
                .map { row ->
                    StockObject(row[StockList.market], row[StockList.stockCode])
                }.toList()
        }

        if (reqStockInfoList.isNotEmpty()) {
            val apiResultList = getInfo(reqStockInfoList)

            apiResultList.filter { infoOutput ->
                !infoOutput.market.isNullOrEmpty()
                        && !infoOutput.stockCode.isNullOrEmpty()
            }
                .forEach { apiResult ->
                    transaction {
                        StockList.update({
                            StockList.market.eq(apiResult.market!!) and
                                    StockList.stockCode.eq(apiResult.stockCode!!)
                        }) {
                            it[StockList.trCrcyCd] = apiResult.trCrcyCd
                            it[StockList.buyUnitQty] = apiResult.buyUnitQty
                            it[StockList.stdPdno] = apiResult.stdPdno
                            it[StockList.prdtName] = if (apiResult.prdtName.contains("]")) {
                                apiResult.prdtName.split(']')[1]
                            } else {
                                apiResult.prdtName
                            }

                            it[StockList.modDate] = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                        }
                    }
                }
        }
    }

    private suspend fun getInfo(targetList: List<StockObject>): List<KisInfoOutput> {
        val apiResultList = mutableListOf<KisInfoOutput>()
        var marketCode: String

        for (stock in targetList) {
            val startTime = System.currentTimeMillis()
            marketCode = if (stock.market == "TSE") "515" else "512"

            val kisApiResult = httpClient.post("/uapi/overseas-price/v1/quotations/search-info") {
                headers {
                    set("tr_id", SEARCH_INFO_TR_ID)
                    set("custtype", "P")
                }

                setBody(
                    KisInfoRequest(
                        marketCode, stock.stockCode
                    )
                )
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
