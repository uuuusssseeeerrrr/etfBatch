package com.ietf.etfbatch.table

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.datetime

object Holiday : Table("etf.holiday") {
    val country = varchar("country", length = 10)
    val holidayDate = varchar("holiday_date", length = 10)
    val year = integer("year")
    val holidayName = varchar("holiday_name", length = 255)
    val holidayType = varchar("holiday_type", length = 50)
    val description = varchar("description", length = 500).nullable()
    val regDate = datetime("reg_date")
    val modDate = datetime("mod_date")

    override val primaryKey = PrimaryKey(country, holidayDate)
}
