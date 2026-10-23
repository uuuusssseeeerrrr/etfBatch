package com.ietf.etfbatch.history.service

import com.ietf.etfbatch.table.BatchJobHistory
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.koin.core.annotation.Single
import kotlin.time.Clock

/**
 * 백그라운드(202 응답) 배치의 실행 결과 기록
 */
@Single
class BatchJobHistoryService {
    fun start(jobName: String): Int {
        return transaction {
            BatchJobHistory.insert {
                it[BatchJobHistory.jobName] = jobName
                it[BatchJobHistory.startDate] = now()
            } get BatchJobHistory.id
        }
    }

    fun finish(id: Int, success: Boolean, message: String? = null) {
        transaction {
            BatchJobHistory.update({ BatchJobHistory.id eq id }) {
                it[BatchJobHistory.successYn] = if (success) "Y" else "N"
                it[BatchJobHistory.message] = message
                it[BatchJobHistory.endDate] = now()
            }
        }
    }

    /**
     * 서버가 실행 도중 종료되어 결과가 남지 않은 배치를 실패로 처리
     */
    fun markInterrupted() {
        transaction {
            BatchJobHistory.update({ BatchJobHistory.successYn.isNull() }) {
                it[BatchJobHistory.successYn] = "N"
                it[BatchJobHistory.message] = "서버 재시작으로 중단됨"
                it[BatchJobHistory.endDate] = now()
            }
        }
    }

    private fun now() = Clock.System.now().toLocalDateTime(TimeZone.of("Asia/Seoul"))
}
