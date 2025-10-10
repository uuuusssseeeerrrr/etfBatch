package com.ietf.etfbatch.stock.service

import com.ietf.etfbatch.httpInf.KisInterface
import com.ietf.etfbatch.stock.dto.KisInfoOutput
import com.ietf.etfbatch.stock.dto.KisInfoRequest
import com.ietf.etfbatch.stock.model.EtfList
import com.ietf.etfbatch.stock.model.StockList
import com.ietf.etfbatch.token.service.KisTokenService
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@Service
@OptIn(ExperimentalTime::class)
class KisInfoService(val kisInterface: KisInterface, val kisTokenService: KisTokenService) {
    companion object {
        const val SEARCH_INFO_TR_ID = "CTPF1702R"
        const val MIN_INTERVAL = 112L
        private val logger = LoggerFactory.getLogger(KisInfoService::class.java)
    }

    fun kisInfo() {
        getEtfInfo()
        getStockInfo()
    }

    /**
     * ETF 정보 가져오기
     */
    private fun getEtfInfo() {
        val reqEtfInfoList = transaction {
            EtfList.select(EtfList.market, EtfList.stockCode)
                .where { (EtfList.stdPdno.isNull()) or (EtfList.stdPdno.trim().eq("")) }
                .map { row ->
                    StockObject(row[EtfList.market], row[EtfList.stockCode])
                }.toList()
        }

        if (reqEtfInfoList.isNotEmpty()) {
            val etfApiResultList = getInfo(reqEtfInfoList)

            transaction {
                etfApiResultList.filter { infoOutput ->
                    !infoOutput.market.isNullOrEmpty()
                            && !infoOutput.stockCode.isNullOrEmpty()
                }
                    .forEach { apiResult ->
                        EtfList.update({
                            EtfList.market.eq(
                                apiResult.market!!
                            ) and EtfList.stockCode.eq(apiResult.stockCode!!)
                        }) {
                            it[EtfList.tradingLot] = apiResult.buyUnitQty
                            it[EtfList.stdPdno] = apiResult.stdPdno
                            it[EtfList.modDate] = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                        }
                    }
            }
        }
    }

    private fun getStockInfo() {
        val reqStockInfoList = transaction {
            StockList.select(StockList.market, StockList.stockCode)
                .where { (StockList.stdPdno.isNull()) or (StockList.stdPdno.trim().eq("")) }
                .map { row ->
                    StockObject(row[StockList.market], row[StockList.stockCode])
                }.toList()
        }

        if (reqStockInfoList.isNotEmpty()) {
            val apiResultList = getInfo(reqStockInfoList)

            transaction {
                apiResultList.filter { infoOutput ->
                    !infoOutput.market.isNullOrEmpty()
                            && !infoOutput.stockCode.isNullOrEmpty()
                }
                    .forEach { apiResult ->
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

    private data class StockObject(
        val market: String,
        val stockCode: String
    )

    private fun getInfo(targetList: List<StockObject>): List<KisInfoOutput> {
        val token = kisTokenService.getKisAccessToken()
        val apiResultList = mutableListOf<KisInfoOutput>()
        var marketCode: String

        for (stock in targetList) {
            val startTime = System.currentTimeMillis()
            marketCode = if (stock.market == "TSE") "515" else "512"

            val kisApiResult = kisInterface.searchInfoApiCall(
                SEARCH_INFO_TR_ID,
                "P",
                "Bearer $token",
                KisInfoRequest(
                    marketCode, stock.stockCode
                ),
            )

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
                Thread.sleep(delayTimeMs)
                logger.debug("stockApiCall -> ${delayTimeMs}ms 지연 후 다음 호출.")
            }
        }

        return apiResultList
    }
}
