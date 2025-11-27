package com.ietf.etfbatch.etf.service.impl

import com.ietf.etfbatch.etf.dto.InvescoData
import com.ietf.etfbatch.etf.service.interfaces.ProcessData
import com.ietf.etfbatch.table.EtfStockListRecord
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.math.BigDecimal

class ProcessInvescoData(val httpClient: HttpClient) : ProcessData {
    companion object {
        val levereageEtfList : Array<String> = arrayOf("QLD")
    }

    override suspend fun processData(data: Map<String, List<String>>): List<EtfStockListRecord> {
        return coroutineScope {
            data.mapNotNull { (key, value) ->
                if (!key.isEmpty() && !value.isEmpty() && value[0] != "" && key !in levereageEtfList) {
                    async(Dispatchers.IO) {
                        getData(key, value[0])
                    }
                } else {
                    null
                }
            }.awaitAll().flatten()
        }
    }

    private suspend fun getData(etfStockCode: String, isin: String): List<EtfStockListRecord> {
        val invescoDataList = httpClient.get(
            "https://dng-api.invesco.com/cache/v1/accounts/en_US/shareclasses/" +
                    isin.substring(2..10) +
                    "/holdings/fund?idType=cusip&productType=ETF"
        ).body<InvescoData>().dataList

        return invescoDataList.mapNotNull { data ->
            if (!data.ticker.isNullOrEmpty()) {
                EtfStockListRecord(
                    "NAS",
                    etfStockCode,
                    data.ticker,
                    data.percentageOfTotalNetAssets.toBigDecimal().divide(
                        BigDecimal(100)
                    )
                )
            } else {
                null
            }
        }
    }
}