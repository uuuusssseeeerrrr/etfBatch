package com.ietf.etfbatch

import com.ietf.etfbatch.etf.service.EtfStocksSyncService
import com.ietf.etfbatch.etf.service.StockListSyncService
import com.ietf.etfbatch.holiday.service.HolidayService
import com.ietf.etfbatch.rate.service.RateService
import com.ietf.etfbatch.stock.service.KisStockInfoService
import com.ietf.etfbatch.stock.service.KisStockPriceService
import com.ietf.etfbatch.stock.service.StockRemoveService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single
import java.util.concurrent.atomic.AtomicBoolean

@Single
data class RoutingClass(
    val kisStockInfoService: KisStockInfoService,
    val kisStockPriceService: KisStockPriceService,
    val stockRemoveService: StockRemoveService,
    val rateService: RateService,
    val etfStocksSyncService: EtfStocksSyncService,
    val stockListSyncService: StockListSyncService,
    val holidayService: HolidayService
)

fun RoutingClass.configureRouting(
    app: Application
) {
    val logger = app.environment.log
    val etfStocksSyncRunning = AtomicBoolean(false)
    val stockListSyncRunning = AtomicBoolean(false)

    /**
     * 나중에 코드가 길어지면 분리하기
     */
    app.routing {
        // 인증 없이 호출 가능한 헬스체크 엔드포인트
        get("/health") {
            call.respondText("OK", status = HttpStatusCode.OK)
        }

        authenticate("tokenAuth") {
            /**
             * 가격조회 API
             */
            post("/prices/etf") {
                val market = call.request.queryParameters["market"]

                if (market.isNullOrBlank()) {
                    call.respondText(
                        text = "market 쿼리 파라미터가 누락되었거나 비어 있습니다.",
                        status = HttpStatusCode.BadRequest
                    )
                    return@post
                }

                try {
                    if (holidayService.isTodayHoliday(market)) {
                        call.respondText("오늘은 휴일이므로 etf 요청을 건너뜁니다. (market: $market)")
                        return@post
                    }
                    if (market == "TSE" && !kisStockPriceService.isTseTradingHours()) {
                        call.respondText("TSE 정규장 시간(09~16시)이 아니므로 etf 요청을 건너뜁니다. (market: $market)")
                        return@post
                    }
                    kisStockPriceService.getEtfPrice(market)
                    call.respondText("etf 요청 처리됨")
                } catch (e: Exception) {
                    logger.error("ETF 가격 조회 중 오류 발생 (market: $market)", e)
                    call.respondText(
                        text = "etf 요청중 오류발생",
                        status = HttpStatusCode.InternalServerError
                    )
                }
            }

            post("/prices/stock") {
                val market = call.request.queryParameters["market"]

                if (market.isNullOrBlank()) {
                    call.respondText(
                        text = "market 쿼리 파라미터가 누락되었거나 비어 있습니다.",
                        status = HttpStatusCode.BadRequest
                    )
                    return@post
                }

                try {
                    if (holidayService.isTodayHoliday(market)) {
                        call.respondText("오늘은 휴일이므로 stock 요청을 건너뜁니다. (market: $market)")
                        return@post
                    }
                    if (market == "TSE" && !kisStockPriceService.isTseTradingHours()) {
                        call.respondText("TSE 정규장 시간(09~16시)이 아니므로 stock 요청을 건너뜁니다. (market: $market)")
                        return@post
                    }
                    kisStockPriceService.getStockPrice(market)
                    call.respondText("stock 요청 처리됨")
                } catch (e: Exception) {
                    logger.error("종목 가격 조회 중 오류 발생 (market: $market)", e)
                    call.respondText(
                        text = "stock 요청중 오류발생",
                        status = HttpStatusCode.InternalServerError
                    )
                }
            }

            /**
             * 환율조회 API
             */
            post("/rate") {
                try {
                    rateService.getRate()
                    call.respondText("환율조회완료")
                } catch (e: Exception) {
                    logger.error("환율 조회 중 오류 발생", e)
                    call.respondText(
                        text = "환율조회중 오류발생",
                        status = HttpStatusCode.InternalServerError
                    )
                }
            }

            /**
             * 클린업 API
             */
            post("/histories/cleanup") {
                try {
                    stockRemoveService.removeOldHistory()
                    call.respondText("오래된데이터 삭제배치 완료")
                } catch (e: Exception) {
                    logger.error("오래된 데이터 삭제 배치 중 오류 발생", e)
                    call.respondText(
                        text = "오래된데이터 삭제배치중 오류발생",
                        status = HttpStatusCode.InternalServerError
                    )
                }
            }

            /**
             * 정보조회 API
             */
            post("/stock-infos") {
                try {
                    kisStockInfoService.kisInfo()
                    call.respondText("kisInfo 요청 처리됨")
                } catch (e: Exception) {
                    logger.error("종목 정보 조회 중 오류 발생", e)
                    call.respondText(
                        text = "kisInfo 요청중 오류발생",
                        status = HttpStatusCode.InternalServerError
                    )
                }
            }

            /**
             * ETF 비중정보 동기화 API
             */
            post("/etf-stocks/sync") {
                // 처리 시간이 서버 읽기 타임아웃을 넘길 수 있어 즉시 응답하고 백그라운드에서 실행
                if (!etfStocksSyncRunning.compareAndSet(false, true)) {
                    call.respondText(
                        text = "ETF 비중정보 동기화가 이미 실행 중입니다.",
                        status = HttpStatusCode.Conflict
                    )
                    return@post
                }

                app.launch {
                    try {
                        logger.info("ETF 비중정보 동기화 시작")
                        etfStocksSyncService.etfStockListInfo()
                        logger.info("ETF 비중정보 동기화 완료")
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        logger.error("ETF 비중정보 동기화 중 오류 발생", e)
                    } finally {
                        etfStocksSyncRunning.set(false)
                    }
                }

                call.respondText("ETF 비중정보 동기화 요청 접수", status = HttpStatusCode.Accepted)
            }

            /**
             * 주식목록 동기화 API
             */
            post("/stock-list/sync") {
                // 처리 시간이 서버 읽기 타임아웃을 넘길 수 있어 즉시 응답하고 백그라운드에서 실행
                if (!stockListSyncRunning.compareAndSet(false, true)) {
                    call.respondText(
                        text = "주식목록 동기화가 이미 실행 중입니다.",
                        status = HttpStatusCode.Conflict
                    )
                    return@post
                }

                app.launch {
                    try {
                        logger.info("주식목록 동기화 시작")
                        stockListSyncService.syncStockList()
                        logger.info("주식목록 동기화 완료")
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        logger.error("주식목록 동기화 중 오류 발생", e)
                    } finally {
                        stockListSyncRunning.set(false)
                    }
                }

                call.respondText("주식목록 동기화 요청 접수", status = HttpStatusCode.Accepted)
            }

            /**
             * 휴일 동기화 API
             */
            get("/holidays/sync") {
                val country = call.request.queryParameters["country"] ?: HolidayService.DEFAULT_COUNTRY

                try {
                    holidayService.syncHolidays(country)
                    call.respondText("휴일 동기화 완료 (country: $country)")
                } catch (e: Exception) {
                    logger.error("휴일 동기화 중 오류 발생 (country: $country)", e)
                    call.respondText(
                        text = "휴일 동기화 중 오류 발생: ${e.message}",
                        status = HttpStatusCode.InternalServerError
                    )
                }
            }
        }
    }
}