package com.ietf.etfbatch.etf.service.interfaces

import com.ietf.etfbatch.table.EtfStockList
import com.ietf.etfbatch.table.EtfStockListRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.DecimalColumnType
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.jdbc.batchUpsert
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.time.Clock

private val logger = LoggerFactory.getLogger(SyncData::class.java)

interface SyncData {
    suspend fun sync()

    /**
     * 데이터베이스에 ETF 종목 리스트를 업서트하고, 15일 이전 데이터를 삭제합니다.
     */
    suspend fun upsertData (dataList: List<EtfStockListRecord>) {
        // 데이터베이스 입력
        val kstTimezone = TimeZone.of("Asia/Seoul")
        val validDataList = fitEtfPercent(dataList)

        withContext(Dispatchers.IO) {
            transaction {
                EtfStockList.batchUpsert(validDataList) { data ->
                    this[EtfStockList.market] = data.market
                    this[EtfStockList.etfStockCode] = data.etfStockCode
                    this[EtfStockList.stockCode] = data.stockCode
                    this[EtfStockList.etfPercent] = data.etfPercent
                    this[EtfStockList.checkDate] = Clock.System.now().toLocalDateTime(kstTimezone)
                }

                EtfStockList.deleteWhere {
                    EtfStockList.checkDate less Clock.System.now()
                        .minus(15, DateTimeUnit.DAY, kstTimezone)
                        .toLocalDateTime(kstTimezone)
                }
            }
        }
    }

    /**
     * etf_percent 값을 컬럼 규격(decimal(precision, scale))에 맞게 반올림하고,
     * 반올림 후에도 정수부 자릿수를 넘는 값은 제외합니다.
     */
    private fun fitEtfPercent(dataList: List<EtfStockListRecord>): List<EtfStockListRecord> {
        val columnType = EtfStockList.etfPercent.columnType as DecimalColumnType
        val scale = columnType.scale
        // decimal(7,5) 기준 최대 99.99999
        val maxValue = BigDecimal.TEN.pow(columnType.precision - scale) - BigDecimal.ONE.movePointLeft(scale)

        return dataList.mapNotNull { data ->
            val percent = data.etfPercent?.setScale(scale, RoundingMode.HALF_UP)

            if (percent != null && percent.abs() > maxValue) {
                logger.warn(
                    "etf_percent 범위 초과로 제외: market={}, etfStockCode={}, stockCode={}, etfPercent={}",
                    data.market, data.etfStockCode, data.stockCode, data.etfPercent
                )
                null
            } else {
                data.copy(etfPercent = percent)
            }
        }
    }
}
