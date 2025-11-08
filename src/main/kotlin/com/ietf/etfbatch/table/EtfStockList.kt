package com.ietf.etfbatch.table

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.datetime
import java.math.BigDecimal

object EtfStockList : Table("etf_stock_list") {
    val market = varchar("market", length = 10).default("''")
    val etfStockCode = varchar("etf_stock_code", length = 20).default("''")
    val stockCode = varchar("stock_code", length = 20).default("''")
    val checkDate = datetime("check_date").nullable()
    val etfPercent = decimal("etf_percent", 7, 5).nullable()
    override val primaryKey = PrimaryKey(market, etfStockCode, stockCode)
}

data class EtfStockListRecord(
    val market: String,
    val etfStockCode: String,
    val stockCode: String,
    val etfPercent: BigDecimal?
)