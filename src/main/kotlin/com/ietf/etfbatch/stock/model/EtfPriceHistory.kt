package com.ietf.etfbatch.stock.model

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.datetime

object EtfPriceHistory : Table("etf_price_history") {
    val market = varchar("market", length = 10)
    val stockCode = varchar("stock_code", length = 20)
    val open = varchar("open", length = 12).nullable()
    val high = varchar("high", length = 12).nullable()
    val low = varchar("low", length = 12).nullable()
    val price = varchar("price", length = 12).nullable()
    val lastDayPrice = varchar("last_day_price", length = 12).nullable()
    val h52p = varchar("h52p", length = 12).nullable()
    val l52p = varchar("l52p", length = 12).nullable()
    val tXprc = varchar("t_xprc", length = 12).nullable()
    val tXdif = varchar("t_xdif", length = 12).nullable()
    val tXrat = varchar("t_xrat", length = 12).nullable()
    val tRate = varchar("t_rate", length = 12).nullable()
    val regDate = datetime("reg_date").nullable()
}