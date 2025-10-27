package com.ietf.etfbatch.etf.service.impl

import com.ietf.etfbatch.etf.service.ProcessData
import com.ietf.etfbatch.table.EtfStockListRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class ProcessSimplexData : ProcessData {
    override suspend fun processData(data: Map<String, List<String>>): List<EtfStockListRecord> {
        return coroutineScope {
            // 1. 총 구매건수 합을 구함
            val totalResult = data.map { (_, value) ->
                async(Dispatchers.Default) {
                    value.map { data ->
                        val splitData = data.split(",")
                        splitData[3].toFloat()
                    }.fold(0f) { acc, i -> acc + i }
                }
            }

            val totalAmount = totalResult.awaitAll().fold(0f) { acc, i -> acc + i }

            // 2. 구매액 / 총합으로 비중 구하기
            val deferedResult = data.map { (key, value) ->
                async(Dispatchers.Default) {
                    value.mapNotNull { data ->
                        val splitData = data.split(",")

                        if (!splitData[3].isEmpty()) {
                            EtfStockListRecord(
                                key,
                                splitData[1].trim().substring(0, 4),
                                splitData[3].toFloat() / totalAmount
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