package com.ietf.etfbatch.etf.service.impl

import com.ietf.etfbatch.etf.dto.EtfPublisher
import com.ietf.etfbatch.etf.service.EtfFileHandler
import com.ietf.etfbatch.etf.service.interfaces.ProcessData
import com.ietf.etfbatch.etf.service.interfaces.SyncData
import com.ietf.etfbatch.etf.util.throwIfFailed
import com.ietf.etfbatch.table.EtfList
import com.ietf.etfbatch.table.EtfStockListRecord
import kotlinx.coroutines.*
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single
import org.slf4j.LoggerFactory
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Single
@Named("japan")
class JapanEtfStockSyncService(
    val etfFileHandler: EtfFileHandler,
    @Named("amova") private val processorAmova: ProcessData,
    @Named("asset") private val processorAsset: ProcessData,
    @Named("globalx") private val processorGlobalx: ProcessData,
    @Named("mitsubishi") private val processorMitsubishi: ProcessData,
    @Named("nomura") private val processorNomura: ProcessData,
    @Named("simplex") private val processorSimplex: ProcessData
) : SyncData {
    companion object {
        private val logger = LoggerFactory.getLogger(JapanEtfStockSyncService::class.java)
    }

    override suspend fun sync() {
        val today = LocalDate.now(ZoneId.of("Asia/Seoul"))

        val etfList = withContext(Dispatchers.IO) {
            transaction {
                EtfList.selectAll().toList()
            }
        }

        // 이전 실행에서 남은 파일이 재사용되지 않도록 먼저 정리
        removeAllDirectories()

        try {
            syncFiles(today, etfList)
        } finally {
            //파일 삭제
//            removeAllDirectories()
        }
    }

    private suspend fun syncFiles(today: LocalDate, etfList: List<ResultRow>) {
        coroutineScope {
            //파일 다운로드
            val deferredDownloads: List<Deferred<Unit>> = etfList
                .map { row ->
                    val companyName = row[EtfList.companyName] ?: ""
                    val stockCode = row[EtfList.stockCode]

                    async(Dispatchers.IO) {
                        when {
                            companyName.startsWith("nomura", true) -> {
                                etfFileHandler.downloadFileToExcelDir(
                                    EtfPublisher.NOMURA.downloadUrl.format(stockCode),
                                    EtfPublisher.NOMURA.folderName,
                                    "${stockCode}_data.xlsx"
                                )
                            }

                            companyName.startsWith("global", true) -> {
                                etfFileHandler.downloadFileToExcelDir(
                                    EtfPublisher.GLOBALX.downloadUrl.format(stockCode),
                                    EtfPublisher.GLOBALX.folderName,
                                    "${stockCode}_data.csv"
                                )
                            }

                            companyName.startsWith("asset", true) -> {
                                etfFileHandler.downloadFileToExcelDir(
                                    EtfPublisher.ASSET.downloadUrl,
                                    EtfPublisher.ASSET.folderName,
                                    "${stockCode}_data.csv"
                                )
                            }

                            companyName.startsWith("mitsu", true) -> {
                                etfFileHandler.downloadFileToExcelDir(
                                    EtfPublisher.MITSUBISHI.downloadUrl.format(stockCode),
                                    EtfPublisher.MITSUBISHI.folderName,
                                    "${stockCode}_data.csv"
                                )
                            }

                            companyName.startsWith("amova", true) -> {
                                etfFileHandler.downloadFileToExcelDir(
                                    EtfPublisher.AMOVA.downloadUrl.format(
                                        stockCode, today.format(DateTimeFormatter.ofPattern("yyyyMM"))
                                    ),
                                    EtfPublisher.AMOVA.folderName,
                                    "${stockCode}_data.xls"
                                )
                            }

                            companyName.startsWith("simplex", true) -> {
                                val formatter = DateTimeFormatter.ofPattern("yyyyMMdd")
                                val yyyyMMdd = when (today.dayOfWeek) {
                                    DayOfWeek.SATURDAY -> {
                                        today.plusDays(2).format(formatter)
                                    }

                                    DayOfWeek.SUNDAY -> {
                                        today.plusDays(1).format(formatter)
                                    }

                                    else -> {
                                        today.format(formatter)
                                    }
                                }

                                etfFileHandler.downloadFileToExcelDir(
                                    "${EtfPublisher.SIMPLEX.downloadUrl}${yyyyMMdd}.xlsx",
                                    EtfPublisher.SIMPLEX.folderName,
                                    "${stockCode}_data.xlsx"
                                )
                            }
                        }
                    }
                }

            deferredDownloads.awaitAll()
        }

        val processors = linkedMapOf(
            EtfPublisher.NOMURA to processorNomura,
            EtfPublisher.GLOBALX to processorGlobalx,
            EtfPublisher.ASSET to processorAsset,
            EtfPublisher.MITSUBISHI to processorMitsubishi,
            EtfPublisher.AMOVA to processorAmova,
            EtfPublisher.SIMPLEX to processorSimplex
        )

        val dataList = mutableListOf<EtfStockListRecord>()
        val failures = mutableListOf<Exception>()

        // 엑셀파일 읽기 및 데이터 가공 (운용사별로 실패를 격리)
        for ((publisher, processor) in processors) {
            try {
                val fileData = etfFileHandler.readDirectoryToStringList(publisher.folderName, publisher.skip)
                dataList.addAll(processor.processData(fileData))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.error("${publisher.name} 비중정보 가공 중 오류 발생", e)
                failures.add(e)
            }
        }

        // 데이터베이스 입력
        upsertData(dataList)

        throwIfFailed(failures)
    }

    private fun removeAllDirectories() {
        EtfPublisher.entries.forEach { etfFileHandler.removeDirectory(it.folderName) }
    }
}
