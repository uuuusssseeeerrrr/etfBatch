package com.ietf.etfbatch

import com.ietf.etfbatch.etf.service.EtfStockListInfoService
import com.ietf.etfbatch.rate.service.RateService
import com.ietf.etfbatch.stock.service.KisInfoService
import com.ietf.etfbatch.stock.service.KisStockService
import com.ietf.etfbatch.stock.service.StockRemoveService
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Application.configureRouting() {
    val logger = environment.log
    val kisInfoService by inject<KisInfoService>()
    val kisStockService by inject<KisStockService>()
    val stockRemoveService by inject<StockRemoveService>()
    val rateService by inject<RateService>()
    val etfStockListInfoService by inject<EtfStockListInfoService>()

    routing {
        authenticate("tokenAuth") {
            get("/etf") {
                try {
                    kisStockService.getEtfPrice()
                } catch (e: Exception) {
                    logger.error(e.message, e)
                    call.respondText(
                        "etf 요청중 오류발생"
                    )
                }

                call.respondText("etf 요청 처리됨")
            }

            get("/stock") {
                try {
                    kisStockService.getStockPrice()
                } catch (e: Exception) {
                    logger.error(e.message, e)
                    call.respondText(
                        "stock 요청중 오류발생"
                    )
                }

                call.respondText("stock 요청 처리됨")
            }

            get("/kisInfo") {
                try {
                    kisInfoService.kisInfo()
                } catch (e: Exception) {
                    logger.error(e.message, e)
                    call.respondText(
                        "kisInfo 요청중 오류발생"
                    )
                }

                call.respondText("kisInfo 요청 처리됨")
            }

            get("/removeOldHistory") {
                try {
                    stockRemoveService.removeOldHistory()
                } catch (e: Exception) {
                    logger.error(e.message, e)
                    call.respondText(
                        "오래된데이터 삭제완료중 오류발생"
                    )
                }

                call.respondText("오래된데이터 삭제완료")
            }

            get("/rate") {
                try {
                    rateService.getRate()
                } catch (e: Exception) {
                    logger.error(e.message, e)
                    call.respondText(
                        "환율조회중 오류발생"
                    )
                }

                call.respondText("환율조회완료")
            }

            get("/etfStock") {
                try {
                    etfStockListInfoService.etfStockListInfo()
                } catch (e: Exception) {
                    logger.error(e.message, e)
                    call.respondText(
                        "ETF 비중정보 입력중 오류발생"
                    )
                }

                call.respondText("ETF 비중정보 입력완료")
            }
        }
    }
}