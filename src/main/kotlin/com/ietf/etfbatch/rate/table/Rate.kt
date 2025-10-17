package com.ietf.etfbatch.rate.table

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.datetime

object Rate : Table("etf.rate") {
    val regDate = datetime("reg_date")
    val usdRate = decimal("usd_rate", 6, 2).nullable()
    val jpyRate = decimal("jpy_rate", 6, 2).nullable()
    val eurRate = decimal("eur_rate", 6, 2).nullable()
    val sgdRate = decimal("sgd_rate", 6, 2).nullable()
}