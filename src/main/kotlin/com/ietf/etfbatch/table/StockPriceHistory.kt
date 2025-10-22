package com.ietf.etfbatch.table

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.datetime

object StockPriceHistory : Table("stock_price_history") {
    val market = varchar("market", length = 10)
    val stockCode = varchar("stock_code", length = 20)
    val open = varchar("open", length = 12).nullable()
    val high = varchar("high", length = 12).nullable()
    val low = varchar("low", length = 12).nullable()
    val price = varchar("price", length = 12).nullable()
    val lastDayPrice = varchar("last_day_price", length = 12).nullable()
    val tomv = varchar("tomv", length = 16).nullable()
    val h52p = varchar("h52p", length = 12).nullable()
    val l52p = varchar("l52p", length = 12).nullable()
    val perx = varchar("perx", length = 10).nullable()
    val pbrx = varchar("pbrx", length = 10).nullable()
    val epsx = varchar("epsx", length = 10).nullable()
    val bpsx = varchar("bpsx", length = 10).nullable()
    val tXprc = varchar("t_xprc", length = 12).nullable()
    val tXdif = varchar("t_xdif", length = 12).nullable()
    val tXrat = varchar("t_xrat", length = 12).nullable()
    val tRate = varchar("t_rate", length = 12).nullable()
    val eIcod = varchar("e_icod", length = 50).nullable()
    val regDate = datetime("reg_date").nullable()
}