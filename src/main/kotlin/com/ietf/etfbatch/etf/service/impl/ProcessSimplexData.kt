package com.ietf.etfbatch.etf.service.impl

import com.ietf.etfbatch.etf.service.interfaces.ProcessData
import com.ietf.etfbatch.table.EtfStockListRecord
import kotlinx.coroutines.*
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single
import java.math.BigDecimal
import java.math.RoundingMode

@Single
@Named("simplex")
class ProcessSimplexData : ProcessData {
    override suspend fun processData(data: Map<String, List<String>>): List<EtfStockListRecord> {
        return coroutineScope {
            val deferedResult = data.map { (key, value) ->
                async(Dispatchers.Default) {
                    // 1. 종목코드, 구매건수 추출
                    val rows = value.mapNotNull { data ->
                        val splitData = data.split(",")
                        val stockCode = splitData.getOrNull(1)?.trim()
                        val amount = splitData.getOrNull(3)?.toBigDecimalOrNull()

                        if (stockCode != null && stockCode.length >= 4 && amount != null) {
                            stockCode.substring(0, 4) to amount
                        } else {
                            null
                        }
                    }

                    // 2. ETF별 총 구매건수 합을 구함
                    val totalAmount = rows.fold(BigDecimal.ZERO) { acc, (_, amount) -> acc + amount }

                    if (totalAmount.signum() == 0) {
                        return@async emptyList()
                    }

                    // 3. 구매건수 / 총합으로 비중 구하기
                    rows.map { (stockCode, amount) ->
                        EtfStockListRecord(
                            "TSE",
                            key,
                            stockCode,
                            amount.divide(totalAmount, 8, RoundingMode.HALF_UP)
                        )
                    }
                }
            }

            deferedResult.awaitAll().flatten()
        }
    }
}
