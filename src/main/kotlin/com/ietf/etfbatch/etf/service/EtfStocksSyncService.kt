package com.ietf.etfbatch.etf.service

import com.ietf.etfbatch.etf.dto.EtfPublisher
import com.ietf.etfbatch.table.EtfList
import com.ietf.etfbatch.table.EtfStockList
import com.ietf.etfbatch.table.EtfStockListRecord
import jakarta.inject.Named
import kotlinx.coroutines.*
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.jdbc.batchUpsert
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.koin.core.component.KoinComponent
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class EtfStocksSyncService(
    val etfFileHandler: EtfFileHandler,
    @param:Named("amova") private val processorAmova: ProcessData,
    @param:Named("asset") private val processorAsset: ProcessData,
    @param:Named("globalx") private val processorGlobalx: ProcessData,
    @param:Named("mitsubishi") private val processorMitsubishi: ProcessData,
    @param:Named("nomura") private val processorNomura: ProcessData,
    @param:Named("simplex") private val processorSimplex: ProcessData,
) : KoinComponent {
    @OptIn(ExperimentalTime::class)
    suspend fun etfStockListInfo() {
        val today = LocalDate.now(ZoneId.of("Asia/Seoul"))

        val etfList = withContext(Dispatchers.IO) {
            transaction {
                EtfList.selectAll().toList()
            }
        }

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

            val dataList = mutableListOf<EtfStockListRecord>()

            // 엑셀파일 읽기
            val nomuraData =
                etfFileHandler.readDirectoryToStringList(EtfPublisher.NOMURA.folderName, EtfPublisher.NOMURA.skip)
            val globalData =
                etfFileHandler.readDirectoryToStringList(EtfPublisher.GLOBALX.folderName, EtfPublisher.GLOBALX.skip)
            val assetData =
                etfFileHandler.readDirectoryToStringList(EtfPublisher.ASSET.folderName, EtfPublisher.ASSET.skip)
            val mitsuData = etfFileHandler.readDirectoryToStringList(
                EtfPublisher.MITSUBISHI.folderName,
                EtfPublisher.MITSUBISHI.skip
            )
            val amovaData =
                etfFileHandler.readDirectoryToStringList(EtfPublisher.AMOVA.folderName, EtfPublisher.AMOVA.skip)
            val simplexData =
                etfFileHandler.readDirectoryToStringList(EtfPublisher.SIMPLEX.folderName, EtfPublisher.SIMPLEX.skip)

            // 데이터 가공
            dataList.addAll(processorNomura.processData(nomuraData))
            dataList.addAll(processorGlobalx.processData(globalData))
            dataList.addAll(processorAsset.processData(assetData))
            dataList.addAll(processorMitsubishi.processData(mitsuData))
            dataList.addAll(processorAmova.processData(amovaData))
            dataList.addAll(processorSimplex.processData(simplexData))

            // 데이터베이스 입력
            val kstTimezone = TimeZone.of("Asia/Seoul")

            withContext(Dispatchers.IO) {
                transaction {
                    EtfStockList.batchUpsert(dataList) { data ->
                        this[EtfStockList.market] = "TSE"
                        this[EtfStockList.etfStockCode] = data.etfStockCode
                        this[EtfStockList.stockCode] = data.stockCode
                        this[EtfStockList.etfPercent] = data.etfPercent
                        this[EtfStockList.checkDate] = Clock.System.now().toLocalDateTime(kstTimezone)
                    }

                    EtfStockList.deleteWhere {
                        EtfStockList.checkDate less Clock.System.now()
                            .minus(15, DateTimeUnit.DAY, kstTimezone)
                            .toLocalDateTime(kstTimezone)
                    }
                }
            }

            //파일 삭제
            etfFileHandler.removeDirectory(EtfPublisher.NOMURA.folderName)
            etfFileHandler.removeDirectory(EtfPublisher.GLOBALX.folderName)
            etfFileHandler.removeDirectory(EtfPublisher.ASSET.folderName)
            etfFileHandler.removeDirectory(EtfPublisher.MITSUBISHI.folderName)
            etfFileHandler.removeDirectory(EtfPublisher.AMOVA.folderName)
            etfFileHandler.removeDirectory(EtfPublisher.SIMPLEX.folderName)
        }
    }
}