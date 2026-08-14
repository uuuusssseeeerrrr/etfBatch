package com.ietf.etfbatch.etf.service.impl

import com.ietf.etfbatch.etf.service.interfaces.ProcessData
import com.ietf.etfbatch.table.EtfStockListRecord
import kotlinx.coroutines.*
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single
import java.math.BigDecimal

@Single
@Named("asset")
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
                                "TSE",
                                key,
                                splitData[1],
                                BigDecimal(splitData[9].replace("%", ""))
                                    .divide(BigDecimal(100))
                                    .multiply(BigDecimal(10000))
                                    .divide(BigDecimal(10000)),
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