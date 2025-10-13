package com.ietf.etfbatch.stock.table

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.datetime

object StockList : Table("stock_list") {
    val market = varchar("market", length = 10).default("''")
    val stockCode = varchar("stock_code", length = 20).default("''")
    val stockName = varchar("stock_name", length = 255).nullable()
    val trCrcyCd = varchar("tr_crcy_cd", length = 3).nullable()
    val buyUnitQty = varchar("buy_unit_qty", length = 10).nullable()
    val prdtName = varchar("prdt_name", length = 60).nullable()
    val stockComment = text("stock_comment").nullable()
    val stdPdno = varchar("std_pdno", length = 12).nullable()
    val regDate = datetime("reg_date").nullable()
    val modDate = datetime("mod_date").nullable()
    override val primaryKey = PrimaryKey(market, stockCode)
}