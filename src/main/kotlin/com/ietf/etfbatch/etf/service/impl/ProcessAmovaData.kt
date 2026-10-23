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

                        val value = splitData.getOrNull(9)?.replace("%", "")?.toBigDecimalOrNull()
                        val percent = if (value != null && value > BigDecimal(100)) {
                            splitData.getOrNull(10)?.toBigDecimalOrNull()
                        } else {
                            value
                        }

                        if (splitData[0] in type && percent != null) {
                            EtfStockListRecord(
                                "TSE",
                                key,
                                splitData[1],
                                percent
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