package com.ietf.etfbatch.etf.service

import com.ietf.etfbatch.stock.service.StockRemoveService
import com.ietf.etfbatch.table.EtfStockList
import com.ietf.etfbatch.table.StockList
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.time.Clock
import kotlin.time.ExperimentalTime


class StockListSyncService : KoinComponent {
    val stockRemoveService by inject<StockRemoveService>()

    @OptIn(ExperimentalTime::class)
    fun syncStockList() {
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

            newStockList.forEach { row ->
                StockList.insert {
                    it[StockList.market] = row[EtfStockList.market]
                    it[StockList.stockCode] = row[EtfStockList.stockCode]
                    it[StockList.regDate] = today
                }
            }

            stockRemoveService.removeUnusedStockInfo()
        }
    }
}