package com.ietf.etfbatch.etf.service.impl

import com.ietf.etfbatch.etf.service.interfaces.ProcessData
import com.ietf.etfbatch.table.EtfStockListRecord
import kotlinx.coroutines.*
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single
import java.math.BigDecimal
import java.math.RoundingMode

@Single
@Named("globalx")
class ProcessGlobalXData : ProcessData {
    override suspend fun processData(data: Map<String, List<String>>): List<EtfStockListRecord> {
        return coroutineScope {
            val deferedResult = data.map { (key, value) ->
                async(Dispatchers.Default) {
                    // 1. 종목코드, 구매액 추출
                    val rows = value.mapNotNull { data ->
                        val splitData = data.split(",")
                        val sharesAmount = splitData.getOrNull(5)?.toBigDecimalOrNull()
                        val price = splitData.getOrNull(6)?.toBigDecimalOrNull()

                        if (price != null && sharesAmount != null) {
                            splitData[0] to price.multiply(sharesAmount)
                        } else {
                            null
                        }
                    }

                    // 2. ETF별 총 구매액 합을 구함
                    val totalAmount = rows.fold(BigDecimal(0)) { acc, (_, amount) -> acc.add(amount) }

                    if (totalAmount.signum() == 0) {
                        return@async emptyList()
                    }

                    // 3. 구매액 / 총합으로 비중 구하기
                    rows.mapNotNull { (stockCode, amount) ->
                        if (stockCode.isNotBlank()) {
                            EtfStockListRecord(
                                "TSE",
                                key,
                                stockCode,
                                amount.divide(totalAmount, 8, RoundingMode.HALF_UP)
                            )
                        } else {
                            null
                        }
                    }
                }
            }

            deferedResult.awaitAll().flatten()
        }
    }
}
