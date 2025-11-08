package com.ietf.etfbatch.table

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.datetime

object PriceApiCallHistories : Table("etf.price_api_call_history") {
    val market = varchar("market", 3)
    val regDate = datetime("reg_date")
    val esYn: Column<String> = char("es_yn", 1)
    val successYn: Column<String> = char("success_yn", 1)
    override val primaryKey = PrimaryKey(market, regDate)
}