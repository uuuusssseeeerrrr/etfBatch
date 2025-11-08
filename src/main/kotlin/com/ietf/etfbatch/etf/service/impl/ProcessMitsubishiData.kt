package com.ietf.etfbatch.etf.service.impl

import com.ietf.etfbatch.etf.service.interfaces.ProcessData
import com.ietf.etfbatch.etf.util.removeCharEtfName
import com.ietf.etfbatch.table.EtfStockListRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.math.BigDecimal

class ProcessMitsubishiData : ProcessData {
    override suspend fun processData(data: Map<String, List<String>>): List<EtfStockListRecord> {
        return coroutineScope {
            val deferedResult = data.map { (key, value) ->
                async(Dispatchers.Default) {
                    buildList {
                        for (data in value) {
                            if (data.replace(",", "").trim().isEmpty()) {
                                break
                            }

                            val splitData = removeCharEtfName(data).split(",")

                            if (!splitData[1].startsWith("T")
                                && splitData.size > 6
                            ) {
                                val value = splitData[6].replace("%", "")

                                if (value.toFloat() > 0) {
                                    add(
                                        EtfStockListRecord(
                                            "TSE",
                                            key,
                                            splitData[1],
                                            BigDecimal(value)
                                                .divide(BigDecimal(100))
                                                .multiply(BigDecimal(10000))
                                                .divide(BigDecimal(10000))
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            deferedResult.awaitAll().flatten()
        }
    }
}