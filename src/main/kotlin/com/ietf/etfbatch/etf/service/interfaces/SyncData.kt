package com.ietf.etfbatch.etf.service.interfaces

import com.ietf.etfbatch.table.EtfStockList
import com.ietf.etfbatch.table.EtfStockListRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.jdbc.batchUpsert
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

interface SyncData {
    suspend fun sync()

    /**
     * 데이터베이스에 ETF 종목 리스트를 업서트하고, 15일 이전 데이터를 삭제합니다.
     */
    @OptIn(ExperimentalTime::class)
    suspend fun upsertData (dataList: List<EtfStockListRecord>) {
        // 데이터베이스 입력
        val kstTimezone = TimeZone.of("Asia/Seoul")

        withContext(Dispatchers.IO) {
            transaction {
                EtfStockList.batchUpsert(dataList) { data ->
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
}