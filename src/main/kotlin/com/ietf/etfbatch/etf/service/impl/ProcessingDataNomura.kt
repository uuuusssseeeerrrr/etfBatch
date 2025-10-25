package com.ietf.etfbatch.etf.service.impl

import com.ietf.etfbatch.etf.service.ProcessingData
import com.ietf.etfbatch.etf.service.removeCharEtfName
import com.ietf.etfbatch.table.EtfStockListRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class ProcessingDataNomura : ProcessingData {
    override suspend fun process(data: Map<String, List<String>>): List<EtfStockListRecord> {
        return coroutineScope {
            val deferedResult = data.map { (key, value) ->
                async(Dispatchers.Default) {
                    value.mapNotNull { mapData ->
                        val splitData = removeCharEtfName(mapData).split(",")

                        if (splitData.size > 7 && splitData[1][0].isDigit()) {
                            EtfStockListRecord(
                                key,
                                if (splitData[1].length == 5) splitData[1].replace("R", "") else splitData[1],
                                splitData[6].toFloat()
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