package com.ietf.etfbatch

import com.ietf.etfbatch.stock.service.KisInfoService
import com.ietf.etfbatch.stock.service.KisStockService
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Application.configureRouting() {
    val logger = environment.log
    val kisInfoService by inject<KisInfoService>()
    val kisStockService by inject<KisStockService>()

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
        }
    }
}