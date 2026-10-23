package com.ietf.etfbatch.table

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.datetime

object BatchJobHistory : Table("etf.batch_job_history") {
    val id = integer("id").autoIncrement()
    val jobName = varchar("job_name", 50)
    // null: 실행 중, Y: 성공, N: 실패
    val successYn: Column<String?> = char("success_yn", 1).nullable()
    val message = text("message").nullable()
    val startDate = datetime("start_date")
    val endDate = datetime("end_date").nullable()
    override val primaryKey = PrimaryKey(id)
}
