package com.ietf.etfbatch.etf.service.impl

import com.ietf.etfbatch.etf.service.interfaces.ProcessData
import com.ietf.etfbatch.etf.service.interfaces.SyncData
import com.ietf.etfbatch.table.EtfList
import com.ietf.etfbatch.table.EtfStockListRecord
import jakarta.inject.Named
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.time.ExperimentalTime

class UsaEtfStockSyncService(
    @param:Named("invesco") private val processorInvesco: ProcessData,
    @param:Named("proshares") private val processorProShares: ProcessData
) : SyncData {
    @OptIn(ExperimentalTime::class)
    override suspend fun sync() {
        val dataList = mutableListOf<EtfStockListRecord>()
        val etfList = withContext(Dispatchers.IO) {
            transaction {
                EtfList.selectAll().toList()
            }
        }

        val invescoMap = mutableMapOf<String, List<String>>()
        val proSharesMap = mutableMapOf<String, List<String>>()

        etfList.map { row ->
            val companyName = row[EtfList.companyName] ?: ""

            when {
                companyName.startsWith("Invesco", true) -> {
                    invescoMap[row[EtfList.stockCode]] = listOf(row[EtfList.isinCode] ?: "")
                }

                companyName.startsWith("ProShares", true) -> {
                    proSharesMap[row[EtfList.stockCode]] = emptyList()
                }
            }
        }

        dataList.addAll(processorInvesco.processData(invescoMap))
        dataList.addAll(processorProShares.processData(proSharesMap))

        dataList.forEach { println(it) }

        // 데이터베이스 입력
        upsertData(dataList)
    }
}