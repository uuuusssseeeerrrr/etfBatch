package com.ietf.etfbatch.etf.service.impl

import com.ietf.etfbatch.etf.service.ProcessData
import com.ietf.etfbatch.table.EtfStockListRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlin.math.round

class ProcessAssetData : ProcessData {
    override suspend fun processData(data: Map<String, List<String>>): List<EtfStockListRecord> {
        return coroutineScope {
            val deferedResult = data.map { (key, value) ->
                val breakType = setOf("REIT")

                async(Dispatchers.Default) {
                    value.mapNotNull { data ->
                        val splitData = data.split(",")

                        if (splitData[0] in breakType) {
                            EtfStockListRecord(
                                key,
                                splitData[1],
                                round((splitData[9].replace("%", "").toFloat() / 100) * 10000) / 10000
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