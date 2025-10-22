package com.ietf.etfbatch.table

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.datetime

object EtfList : Table("etf_list") {
    val market = varchar("market", length = 10).default("''")
    val stockCode = varchar("stock_code", length = 20).default("''")
    val etfName = varchar("etf_name", length = 255).nullable()
    val companyName = varchar("company_name", length = 50).nullable()
    val benchmarkIndex = varchar("benchmark_index", length = 255).nullable()
    val indexComment = text("index_comment").nullable()
    val tradingLot = varchar("trading_lot", length = 10).nullable()
    val trustFeeRate = varchar("trust_fee_rate", length = 10).nullable()
    val stdPdno = varchar("std_pdno", length = 12).nullable()
    val regDate = datetime("reg_date")
    val modDate = datetime("mod_date")
    override val primaryKey = PrimaryKey(market, stockCode)
}