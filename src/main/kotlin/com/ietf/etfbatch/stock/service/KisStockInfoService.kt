package com.ietf.etfbatch.stock.service

import com.ietf.etfbatch.config.KisConfig
import com.ietf.etfbatch.config.VaultConfig
import com.ietf.etfbatch.stock.dto.JpxSummaryResponse
import com.ietf.etfbatch.stock.dto.KisInfoOutput
import com.ietf.etfbatch.stock.dto.KisInfoResponse
import com.ietf.etfbatch.stock.dto.StockObject
import com.ietf.etfbatch.stock.dto.YahooQuoteSummaryResponse
import com.ietf.etfbatch.stock.enum.PrdtTypeCd
import com.ietf.etfbatch.table.EtfList
import com.ietf.etfbatch.table.StockList
import com.ietf.etfbatch.token.service.KisTokenService
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.koin.core.annotation.Single
import org.slf4j.LoggerFactory
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds

@Single
class KisStockInfoService(
    val httpClient: HttpClient,
    private val kisTokenService: KisTokenService
) {
    companion object {
        const val SEARCH_INFO_TR_ID = "CTPF1702R"
        const val MIN_INTERVAL = 112L
        // Yahoo/JPX 요청 간격 (rate limit 방지)
        private const val SUMMARY_INTERVAL = 500L
        private val logger = LoggerFactory.getLogger(KisStockInfoService::class.java)
        private const val YAHOO_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0 Safari/537.36"
    }

    private var yahooSession: YahooSession? = null
    private val yahooSessionMutex = Mutex()

    suspend fun kisInfo() {
        getEtfInfo()
        getStockInfo()
        getStockSummary()
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
                withContext(Dispatchers.IO) {
                    transaction {
                        apiResultList.forEach { apiResult ->
                            val marketChanged = apiResult.resolvedMarket != apiResult.market

                            if (marketChanged) {
                                val alreadyExists = StockList.select(StockList.stockCode)
                                    .where {
                                        StockList.market.eq(apiResult.resolvedMarket) and
                                                StockList.stockCode.eq(apiResult.stockCode)
                                    }
                                    .any()

                                // 변경될 시장으로 이미 등록된 종목이 있으면 기존 행만 삭제 (PK 중복 방지)
                                if (alreadyExists) {
                                    StockList.deleteWhere {
                                        StockList.market.eq(apiResult.market) and
                                                StockList.stockCode.eq(apiResult.stockCode)
                                    }
                                    logger.info(
                                        "stock_list 중복 행 삭제 - stockCode: {}, market: {} ({}로 이미 등록됨)",
                                        apiResult.stockCode, apiResult.market, apiResult.resolvedMarket
                                    )
                                    return@forEach
                                }

                                logger.info(
                                    "stock_list market 변경 - stockCode: {}, market: {} -> {}",
                                    apiResult.stockCode, apiResult.market, apiResult.resolvedMarket
                                )
                            }

                            StockList.update({
                                StockList.market.eq(apiResult.market) and
                                        StockList.stockCode.eq(apiResult.stockCode)
                            }) {
                                it[StockList.market] = apiResult.resolvedMarket
                                it[StockList.trCrcyCd] = apiResult.trCrcyCd
                                it[StockList.buyUnitQty] = apiResult.buyUnitQty
                                it[StockList.stdPdno] = apiResult.stdPdno
                                it[StockList.prdtName] =
                                    apiResult.prdtName.substringAfter(']').trim().ifBlank { apiResult.prdtName }
                                it[StockList.isinCode] = apiResult.isinCd
                                it[StockList.stockName] =
                                    apiResult.prdtEngName.substringAfter(']').trim().ifBlank { apiResult.prdtEngName }
                                it[StockList.modDate] = today
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 종목 개요가 없는 미국/일본 종목의 개요 가져오기
     * (신규 종목뿐 아니라 이전에 조회 실패한 종목도 매 실행마다 다시 시도)
     */
    suspend fun getStockSummary() {
        val summaryMarkets = listOf(
            PrdtTypeCd.JAPAN.marketCode,
            PrdtTypeCd.USA_NEWYORK.marketCode,
            PrdtTypeCd.USA_NASDAQ.marketCode,
            PrdtTypeCd.USA_AMEX.marketCode
        )

        val targetList = transaction {
            StockList.select(StockList.market, StockList.stockCode)
                .where {
                    StockList.stockComment.isNull() and
                            (StockList.market inList summaryMarkets) and
                            StockList.stdPdno.isNotNull() and
                            (StockList.stdPdno.trim() neq "")
                }
                .map { row ->
                    StockObject(row[StockList.market], row[StockList.stockCode])
                }.toList()
        }

        var successCount = 0
        for (stock in targetList) {
            val summary = getSummary(stock)

            if (summary != null) {
                transaction {
                    StockList.update({
                        StockList.market.eq(stock.market) and
                                StockList.stockCode.eq(stock.stockCode)
                    }) {
                        it[StockList.stockComment] = summary
                    }
                }
                successCount++
            }

            delay(SUMMARY_INTERVAL.milliseconds)
        }

        logger.info("종목 개요 조회 완료 - 대상: {}건, 성공: {}건", targetList.size, successCount)
    }

    private suspend fun getInfo(targetList: List<StockObject>): List<KisInfoOutput> {
        val apiResultList = mutableListOf<KisInfoOutput>()
        val token = kisTokenService.getKisAccessToken()
        val appKey = VaultConfig.getVaultSecret("kis_key")
        val appSecret = VaultConfig.getVaultSecret("kis_secret")

        for (stock in targetList) {
            val startTime = System.currentTimeMillis()
            val prdtTypeCd = PrdtTypeCd.findInfoCodeByMarketCode(stock.market)
            var resolvedMarket = stock.market
            var kisApiResult = requestInfo(token, appKey, appSecret, prdtTypeCd, stock.stockCode)

            // 뉴욕(513)으로 조회되지 않으면 나스닥(512)으로 재조회
            if (kisApiResult.rtCd == "7" && prdtTypeCd == PrdtTypeCd.USA_NEWYORK.infoCode) {
                delay(MIN_INTERVAL.milliseconds)
                kisApiResult = requestInfo(token, appKey, appSecret, PrdtTypeCd.USA_NASDAQ.infoCode, stock.stockCode)
                resolvedMarket = PrdtTypeCd.USA_NASDAQ.marketCode
            }

            if (kisApiResult.rtCd == "0" &&
                kisApiResult.output != null
            ) {
                kisApiResult.output.market = stock.market
                kisApiResult.output.stockCode = stock.stockCode
                kisApiResult.output.resolvedMarket = resolvedMarket

                apiResultList.add(kisApiResult.output)
            }

            val elapsedTimeMs = System.currentTimeMillis() - startTime
            val delayTimeMs = MIN_INTERVAL - elapsedTimeMs
            if (delayTimeMs > 0) {
                // 목표 간격보다 빨리 끝났다면, 남은 시간만큼 대기
                delay(delayTimeMs.milliseconds)
                logger.debug("stockApiCall -> ${delayTimeMs}ms 지연 후 다음 호출.")
            }
        }

        return apiResultList
    }

    private suspend fun requestInfo(
        token: String,
        appKey: String,
        appSecret: String,
        prdtTypeCd: Int,
        stockCode: String
    ): KisInfoResponse {
        return httpClient.get("${KisConfig.BASE_URL}/uapi/overseas-price/v1/quotations/search-info") {
            header("authorization", "Bearer $token")
            header("appkey", appKey)
            header("appsecret", appSecret)
            header("tr_id", SEARCH_INFO_TR_ID)
            header("custtype", "P")

            url {
                parameters.append("PRDT_TYPE_CD", prdtTypeCd.toString())
                parameters.append("PDNO", stockCode)
            }
        }.body<KisInfoResponse>()
    }

    /**
     * 종목 개요 가져오기 (실패해도 배치가 중단되지 않도록 null 반환)
     */
    private suspend fun getSummary(stock: StockObject): String? {
        return try {
            when (stock.market) {
                PrdtTypeCd.JAPAN.marketCode ->
                    requestJPStockSummary(stock.stockCode).section1?.data?.get(stock.stockCode)?.summary
                        ?.replace("\n", "")

                PrdtTypeCd.USA_NEWYORK.marketCode,
                PrdtTypeCd.USA_NASDAQ.marketCode,
                PrdtTypeCd.USA_AMEX.marketCode -> requestUSStockSummary(stock.stockCode)

                else -> null
            }?.takeIf { it.isNotBlank() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.warn("종목 개요 조회 실패 - stockCode: {}, market: {}", stock.stockCode, stock.market, e)
            null
        }
    }

    /**
     * Yahoo quoteSummary API로 미국 종목 개요 가져오기
     * (프로필 페이지 HTML은 서버 렌더링 시 개요가 누락되는 경우가 있어 API를 직접 호출)
     */
    private suspend fun requestUSStockSummary(
        stockCode: String
    ): String? {
        val session = getYahooSession()
        var response = requestYahooQuoteSummary(stockCode, session)

        // crumb이 만료되면 쿠키/crumb을 새로 받아 한 번 재시도
        if (response.quoteSummary?.error?.code == "Unauthorized") {
            invalidateYahooSession(session)
            response = requestYahooQuoteSummary(stockCode, getYahooSession())
        }

        response.quoteSummary?.error?.let { error ->
            logger.warn("Yahoo 종목 개요 조회 실패 - stockCode: {}, error: {} ({})", stockCode, error.code, error.description)
        }

        return response.quoteSummary?.result?.firstOrNull()?.assetProfile?.longBusinessSummary
    }

    private suspend fun requestYahooQuoteSummary(
        stockCode: String,
        session: YahooSession
    ): YahooQuoteSummaryResponse {
        return httpClient.get("https://query1.finance.yahoo.com/v10/finance/quoteSummary/$stockCode") {
            header("User-Agent", YAHOO_USER_AGENT)
            header("Cookie", session.cookie)
            url {
                parameters.append("modules", "assetProfile")
                parameters.append("crumb", session.crumb)
            }
        }.body<YahooQuoteSummaryResponse>()
    }

    /**
     * quoteSummary API 호출에 필요한 쿠키와 crumb (한 번 받으면 재사용)
     */
    private suspend fun getYahooSession(): YahooSession = yahooSessionMutex.withLock {
        yahooSession ?: createYahooSession().also { yahooSession = it }
    }

    /**
     * 만료된 세션 폐기 (다른 코루틴이 이미 새 세션으로 교체했으면 유지)
     */
    private suspend fun invalidateYahooSession(expired: YahooSession) = yahooSessionMutex.withLock {
        if (yahooSession == expired) yahooSession = null
    }

    private suspend fun createYahooSession(): YahooSession {
        // fc.yahoo.com은 404를 반환하지만 응답에 A3 쿠키를 내려줌
        val cookie = httpClient.get("https://fc.yahoo.com") {
            header("User-Agent", YAHOO_USER_AGENT)
        }.headers.getAll("Set-Cookie").orEmpty()
            .joinToString("; ") { it.substringBefore(";") }

        check(cookie.isNotBlank()) { "Yahoo 쿠키를 받지 못했습니다." }

        val crumbResponse = httpClient.get("https://query1.finance.yahoo.com/v1/test/getcrumb") {
            header("User-Agent", YAHOO_USER_AGENT)
            header("Cookie", cookie)
            // ContentNegotiation이 붙이는 Accept: application/json 으로는 406이 반환되어 text/plain 지정
            header(HttpHeaders.Accept, ContentType.Text.Plain.toString())
        }
        val crumb = crumbResponse.bodyAsText().trim()

        // 429 등 오류 응답 본문("Too Many Requests")이 crumb으로 저장되지 않도록 확인
        check(crumbResponse.status == HttpStatusCode.OK && crumb.isNotBlank() && !crumb.contains(' ')) {
            "Yahoo crumb 발급 실패 - status: ${crumbResponse.status}"
        }

        return YahooSession(cookie, crumb)
    }

    private data class YahooSession(val cookie: String, val crumb: String)

    private suspend fun requestJPStockSummary(
        stockCode: String
    ): JpxSummaryResponse {
        return httpClient.get("https://quote.jpx.co.jp/jpxhp/jcgi/wrap/qjsonp.aspx") {
            url {
                parameters.append("F", "ctl/bus_summary")
                parameters.append("qcode", stockCode)
            }
        }.body<JpxSummaryResponse>()
    }
}
