package com.ietf.etfbatch.etf.service.impl

import com.ietf.etfbatch.etf.service.interfaces.ProcessData
import com.ietf.etfbatch.etf.util.removeCharEtfName
import com.ietf.etfbatch.table.EtfStockListRecord
import kotlinx.coroutines.*
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single
import java.math.BigDecimal

@Single
@Named("nomura")
class ProcessNomuraData : ProcessData {
    override suspend fun processData(data: Map<String, List<String>>): List<EtfStockListRecord> {
        return coroutineScope {
            val deferedResult = data.map { (key, value) ->
                async(Dispatchers.Default) {
                    value.mapNotNull { mapData ->
                        val splitData = removeCharEtfName(mapData).split(",")

                        if (splitData.size > 7 && splitData[1][0].isDigit()) {
                            EtfStockListRecord(
                                "TSE",
                                key,
                                if (splitData[1].length == 5) splitData[1].replace("R", "") else splitData[1],
                                BigDecimal(splitData[6])
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