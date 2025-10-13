package com.ietf.etfbatch.stock.table

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.datetime

object EtfStockList : Table("etf_stock_list") {
    val market = varchar("market", length = 10).default("''")
    val etfStockCode = varchar("etf_stock_code", length = 20).default("''")
    val stockCode = varchar("stock_code", length = 20).default("''")
    val regDate = datetime("reg_date").nullable()
    val etfPercent = float("etf_percent").nullable()
    override val primaryKey = PrimaryKey(market, etfStockCode, stockCode)
}