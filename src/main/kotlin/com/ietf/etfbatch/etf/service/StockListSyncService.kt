package com.ietf.etfbatch.etf.service

import com.ietf.etfbatch.stock.service.KisStockInfoService
import com.ietf.etfbatch.stock.service.StockRemoveService
import com.ietf.etfbatch.table.EtfStockList
import com.ietf.etfbatch.table.StockList
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.time.Clock
import kotlin.time.ExperimentalTime


class StockListSyncService(
    val stockRemoveService: StockRemoveService,
    val kisStockInfoService: KisStockInfoService
) {
    @OptIn(ExperimentalTime::class)
    suspend fun syncStockList() {
        val today = Clock.System.now().toLocalDateTime(TimeZone.of("Asia/Seoul"))

        transaction {
            val newStockList = EtfStockList
                .join(
                    StockList,
                    JoinType.LEFT,
                    additionalConstraint = {
                        (EtfStockList.market eq StockList.market) and
                                (EtfStockList.stockCode eq StockList.stockCode)
                    }
                )
                .select(EtfStockList.market, EtfStockList.stockCode)
                .where { StockList.stockCode.isNull() }
                .withDistinct()
                .toList()

            if (newStockList.isNotEmpty()) {
                StockList.batchInsert(newStockList) { row ->
                    this[StockList.market] = row[EtfStockList.market]
                    this[StockList.stockCode] = row[EtfStockList.stockCode]
                    this[StockList.regDate] = today
                    this[StockList.modDate] = today
                }
            }

            stockRemoveService.removeUnusedStockInfo()
        }

        kisStockInfoService.getStockInfo()

        transaction {
            stockRemoveService.removeUnCorrectedStockInfo()
        }
    }
}