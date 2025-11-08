package com.ietf.etfbatch.etf.service.impl

import com.ietf.etfbatch.etf.service.interfaces.ProcessData
import com.ietf.etfbatch.table.EtfStockListRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.math.BigDecimal

class ProcessProSharesData : ProcessData {
    override suspend fun processData(data: Map<String, List<String>>): List<EtfStockListRecord> {
        return coroutineScope {
            val deferedResult = data.mapNotNull { (key, _) ->
                when (key) {
                    "QLD" -> async(Dispatchers.IO) {
                        scrapData(
                            etfStockCode = key,
                            url = "https://www.proshares.com/our-etfs/leveraged-and-inverse/${key.lowercase()}"
                        )
                    }

                    else -> null
                }
            }

            deferedResult.awaitAll().flatten()
        }
    }

    private fun scrapData(etfStockCode: String, url: String): List<EtfStockListRecord> {
        val resultList = mutableListOf<EtfStockListRecord>()
        val doc: Document = Jsoup.connect(url).timeout(10 * 1000).post()
        val body: Element = doc.body()

        body.select("#holdings tbody tr").map { itemElement ->
            val ticker = itemElement.select("td").eq(1).text()

            if (ticker != "" && ticker != "--") {
                resultList.add(
                    EtfStockListRecord(
                        "NYS",
                        etfStockCode = etfStockCode,
                        stockCode = ticker,
                        etfPercent = itemElement.select("td")
                            .eq(0).text().replace("%", "")
                            .toBigDecimal()
                            .divide(BigDecimal(100))
                    )
                )
            }
        }

        return resultList
    }
}