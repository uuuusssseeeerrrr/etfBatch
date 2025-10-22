package com.ietf.etfbatch.table

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Table

object Token : Table("token") {
    val regDate: Column<String> = varchar("reg_date", 8)
    val token: Column<String> = text("token")
}