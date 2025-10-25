package com.ietf.etfbatch.etf.service.impl

import com.ietf.etfbatch.etf.service.ProcessingData
import com.ietf.etfbatch.table.EtfStockListRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class ProcessingDataAmova : ProcessingData {
    override suspend fun process(data: Map<String, List<String>>): List<EtfStockListRecord> {
        return coroutineScope {
            val deferedResult = data.map { (key, value) ->
                val type = setOf("REIT/ETF", "Stock")

                async(Dispatchers.Default) {
                    value.mapNotNull { data ->
                        val splitData = data.split(",")

                        if (splitData[0] in type) {
                            val value = splitData[9].toFloat()
                            EtfStockListRecord(
                                key,
                                splitData[1],
                                if (value > 100) splitData[10].toFloat() else value
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