package com.ietf.etfbatch.etf.service.impl

import com.ietf.etfbatch.etf.service.interfaces.ProcessData
import com.ietf.etfbatch.etf.service.interfaces.SyncData
import com.ietf.etfbatch.etf.util.throwIfFailed
import com.ietf.etfbatch.table.EtfList
import com.ietf.etfbatch.table.EtfStockListRecord
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single
import org.slf4j.LoggerFactory

@Single
@Named("usa")
class UsaEtfStockSyncService(
    @Named("invesco") private val processorInvesco: ProcessData,
    @Named("proshares") private val processorProShares: ProcessData
) : SyncData {
    companion object {
        private val logger = LoggerFactory.getLogger(UsaEtfStockSyncService::class.java)
    }

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

        val failures = mutableListOf<Exception>()

        // 운용사별로 실패를 격리
        listOf(
            Triple("Invesco", processorInvesco, invescoMap),
            Triple("ProShares", processorProShares, proSharesMap)
        ).forEach { (name, processor, map) ->
            try {
                dataList.addAll(processor.processData(map))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.error("$name 비중정보 가공 중 오류 발생", e)
                failures.add(e)
            }
        }

        logger.info("미국 ETF 비중정보 가공 완료 - 총 {}건", dataList.size)

        // 데이터베이스 입력
        upsertData(dataList)

        throwIfFailed(failures)
    }
}