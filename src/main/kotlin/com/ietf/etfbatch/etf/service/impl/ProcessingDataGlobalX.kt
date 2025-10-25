package com.ietf.etfbatch.etf.service.impl

import com.ietf.etfbatch.etf.service.ProcessingData
import com.ietf.etfbatch.table.EtfStockListRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.math.BigDecimal
import java.math.RoundingMode

class ProcessingDataGlobalX : ProcessingData {
    override suspend fun process(data: Map<String, List<String>>): List<EtfStockListRecord> {
        return coroutineScope {
            // 1. 총 구매액 합을 구함
            val totalResult = data.map { (_, value) ->
                async(Dispatchers.Default) {
                    value.map { data ->
                        val splitData = data.split(",")
                        splitData[5].toBigDecimal() * splitData[6].toBigDecimal()
                    }.fold(BigDecimal(0)) { acc, bigDecimal -> acc.add(bigDecimal) }
                }
            }

            val totalAmount = totalResult.awaitAll().fold(BigDecimal(0)) { acc, bigDecimal -> acc.add(bigDecimal) }

            // 2. 구매액 / 총합으로 비중 구하기
            val deferedResult = data.map { (key, value) ->
                async(Dispatchers.Default) {
                    value.mapNotNull { data ->
                        val splitData = data.split(",")

                        if (splitData.size > 5 && !splitData[0].isBlank()) {
                            EtfStockListRecord(
                                key,
                                splitData[0],
                                (splitData[5].toBigDecimal() * splitData[6].toBigDecimal())
                                    .divide(totalAmount, 8, RoundingMode.HALF_UP).toFloat()
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