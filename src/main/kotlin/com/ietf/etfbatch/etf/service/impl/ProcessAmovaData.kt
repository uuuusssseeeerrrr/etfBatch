package com.ietf.etfbatch.etf.service.impl

import com.ietf.etfbatch.etf.service.interfaces.ProcessData
import com.ietf.etfbatch.table.EtfStockListRecord
import kotlinx.coroutines.*
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single
import java.math.BigDecimal

@Single
@Named("amova")
class ProcessAmovaData : ProcessData {
    override suspend fun processData(data: Map<String, List<String>>): List<EtfStockListRecord> {
        return coroutineScope {
            val deferedResult = data.map { (key, value) ->
                val type = setOf("REIT/ETF", "Stock")

                async(Dispatchers.Default) {
                    value.mapNotNull { data ->
                        val splitData = data.split(",")

                        if (splitData[0] in type) {
                            val value = splitData[9].replace("%", "")
                            EtfStockListRecord(
                                "TSE",
                                key,
                                splitData[1],
                                BigDecimal(if (value.toDouble() > 100) splitData[10] else value)
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