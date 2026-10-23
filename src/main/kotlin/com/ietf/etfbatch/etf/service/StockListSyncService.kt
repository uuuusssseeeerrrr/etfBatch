package com.ietf.etfbatch.etf.service

import com.ietf.etfbatch.stock.service.KisStockInfoService
import com.ietf.etfbatch.stock.service.StockRemoveService
import com.ietf.etfbatch.table.EtfStockList
import com.ietf.etfbatch.table.StockList
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.koin.core.annotation.Single
import kotlin.time.Clock

@Single
class StockListSyncService(
    val stockRemoveService: StockRemoveService,
    val kisStockInfoService: KisStockInfoService
) {
    suspend fun syncStockList() {
        val today = Clock.System.now().toLocalDateTime(TimeZone.of("Asia/Seoul"))

        transaction {
            val newStockList = EtfStockList
                .join(
                    StockList,
                    JoinType.LEFT,
                    // KIS 재조회로 stock_list의 market이 바뀔 수 있어 종목코드로만 비교
                    additionalConstraint = {
                        EtfStockList.stockCode eq StockList.stockCode
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