package com.ietf.etfbatch

import com.ietf.etfbatch.etf.service.EtfStocksSyncService
import com.ietf.etfbatch.etf.service.StockListSyncService
import com.ietf.etfbatch.rate.service.RateService
import com.ietf.etfbatch.stock.service.KisStockInfoService
import com.ietf.etfbatch.stock.service.KisStockPriceService
import com.ietf.etfbatch.stock.service.StockRemoveService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.logging.*
import org.koin.ktor.ext.inject

fun Application.configureRouting() {
    val logger = environment.log
    val kisStockInfoService by inject<KisStockInfoService>()
    val kisStockPriceService by inject<KisStockPriceService>()
    val stockRemoveService by inject<StockRemoveService>()
    val rateService by inject<RateService>()
    val etfStocksSyncService by inject<EtfStocksSyncService>()
    val stockListSyncService by inject<StockListSyncService>()

    /**
     * 나중에 코드가 길어지면 분리하기
     */
    routing {
        authenticate("tokenAuth") {
            post("/prices/etf") {
                try {
                    kisStockPriceService.getEtfPrice()
                } catch (e: Exception) {
                    logger.error(e)
                    call.respondText(
                        text = "etf 요청중 오류발생",
                        status = HttpStatusCode.InternalServerError
                    )

                    return@post
                }

                call.respondText("etf 요청 처리됨")
            }

            post("/prices/stock") {
                try {
                    kisStockPriceService.getStockPrice()
                } catch (e: Exception) {
                    logger.error(e)
                    call.respondText(
                        text = "stock 요청중 오류발생",
                        status = HttpStatusCode.InternalServerError
                    )

                    return@post
                }

                call.respondText("stock 요청 처리됨")
            }

            post("/stock-infos") {
                try {
                    kisStockInfoService.kisInfo()
                } catch (e: Exception) {
                    logger.error(e)
                    call.respondText(
                        text = "kisInfo 요청중 오류발생",
                        status = HttpStatusCode.InternalServerError
                    )

                    return@post
                }

                call.respondText("kisInfo 요청 처리됨")
            }

            post("/histories/cleanup") {
                try {
                    stockRemoveService.removeOldHistory()
                } catch (e: Exception) {
                    logger.error(e)
                    call.respondText(
                        text = "오래된데이터 삭제배치중 오류발생",
                        status = HttpStatusCode.InternalServerError
                    )

                    return@post
                }

                call.respondText("오래된데이터 삭제배치 완료")
            }

            post("/rate") {
                try {
                    rateService.getRate()
                } catch (e: Exception) {
                    logger.error(e)
                    call.respondText(
                        text = "환율조회중 오류발생",
                        status = HttpStatusCode.InternalServerError
                    )

                    return@post
                }

                call.respondText("환율조회완료")
            }

            post("/etf-stocks/sync") {
                try {
                    etfStocksSyncService.etfStockListInfo()
                } catch (e: Exception) {
                    logger.error(e)
                    call.respondText(
                        text = "ETF 비중정보 입력중 오류발생",
                        status = HttpStatusCode.InternalServerError
                    )

                    return@post
                }

                call.respondText("ETF 비중정보 입력완료")
            }

            post("/stock-list/sync") {
                try {
                    stockListSyncService.syncStockList()
                } catch (e: Exception) {
                    logger.error(e)
                    call.respondText(
                        text = "주식목록 동기화중 오류발생",
                        status = HttpStatusCode.InternalServerError
                    )

                    return@post
                }

                call.respondText("주식목록 동기화 완료")
            }
        }
    }
}