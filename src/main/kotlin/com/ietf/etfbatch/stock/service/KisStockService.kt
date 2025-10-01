package com.ietf.etfbatch.stock.service

import com.ietf.etfbatch.httpInf.KisInterface
import com.ietf.etfbatch.stock.dto.KisPriceDetailOutput
import com.ietf.etfbatch.stock.dto.KisPriceDetailResquest
import com.ietf.etfbatch.stock.model.EtfList
import com.ietf.etfbatch.stock.model.EtfPriceHistory
import com.ietf.etfbatch.token.service.KisTokenService
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.springframework.stereotype.Service

@Service
class KisStockService(val kisInterface: KisInterface, val kisTokenService: KisTokenService) {
    companion object {
        const val PRICE_DETAIL_TR_ID = "HHDFS76200200"
    }

    /**
     * ETF 가격정보 가져오기
     */
    fun getEtfPrice() {
        transaction {
            val targetEtfList = EtfList.selectAll().toList()
            val etfApiResultList = mutableListOf<KisPriceDetailOutput>()

            if (targetEtfList.isNotEmpty()) {
                val token = kisTokenService.getKisAccessToken()

                for (etf in targetEtfList) {
                    val kisPriceApiResult = kisInterface.priceDetailApiCall(
                        PRICE_DETAIL_TR_ID,
                        token,
                        KisPriceDetailResquest(etf[EtfList.market], etf[EtfList.stockCode])
                    )

                    if(kisPriceApiResult.rtCd == "0") {
                        kisPriceApiResult.output.market = etf[EtfList.market]
                        kisPriceApiResult.output.stockCode = etf[EtfList.stockCode]
                        etfApiResultList.add(kisPriceApiResult.output)
                    }
                }

                EtfPriceHistory.batchInsert(etfApiResultList, shouldReturnGeneratedValues = false) {etfApiResult ->
                    this[EtfPriceHistory.market] = etfApiResult.market
                    this[EtfPriceHistory.stockCode] = etfApiResult.stockCode
                    this[EtfPriceHistory.open] = etfApiResult.open
                    this[EtfPriceHistory.high] = etfApiResult.high
                    this[EtfPriceHistory.low] = etfApiResult.low
                    this[EtfPriceHistory.price] = etfApiResult.last
                    this[EtfPriceHistory.lastDayPrice] = etfApiResult.base
                    this[EtfPriceHistory.h52p] = etfApiResult.h52p
                    this[EtfPriceHistory.l52p] = etfApiResult.l52p
                    this[EtfPriceHistory.tXprc] = etfApiResult.t_xprc
                    this[EtfPriceHistory.tXdif] = etfApiResult.t_xdif
                    this[EtfPriceHistory.tXrat] = etfApiResult.t_xrat
                    this[EtfPriceHistory.tRate] = etfApiResult.t_rate
                }
            }
        }
    }
}