package com.ietf.etfbatch.stock.service

import com.ietf.etfbatch.table.EtfPriceHistory
import com.ietf.etfbatch.table.StockPriceHistory
import com.ietf.etfbatch.table.Token
import kotlinx.datetime.*
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class StockRemoveService() {
    /**
     * 일주일 이상 지난 히스토리 삭제
     */
    fun removeOldHistory() {
        val targetDate = Clock.System.now().minus(7, DateTimeUnit.DAY, TimeZone.of("Asia/Seoul"))
            .toLocalDateTime(TimeZone.of("Asia/Seoul"))

        val targetDateFormat = """
            ${targetDate.year}
            ${targetDate.month.number.toString().padStart(2, '0')}
            ${targetDate.day.toString().padStart(2, '0')}
        """.trimIndent().replace("\n", "")

        transaction {
            EtfPriceHistory.deleteWhere {
                EtfPriceHistory.regDate.lessEq(targetDate)
            }

            StockPriceHistory.deleteWhere {
                StockPriceHistory.regDate.lessEq(targetDate)
            }

            Token.deleteWhere {
                Token.regDate.lessEq(targetDateFormat)
            }
        }
    }
}
