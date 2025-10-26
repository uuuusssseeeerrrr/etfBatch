package com.ietf.etfbatch.etf.service

import com.ietf.etfbatch.etf.dto.EtfPublisher
import com.ietf.etfbatch.table.EtfList
import com.ietf.etfbatch.table.EtfStockList
import com.ietf.etfbatch.table.EtfStockListRecord
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.jdbc.batchUpsert
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named
import org.mozilla.universalchardet.ReaderFactory
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.Charset
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.io.path.extension
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class EtfStockListInfoService : KoinComponent {
    companion object {
        val excelDir: String = System.getenv("EXCEL_DIR") ?: "/home/rocky/excel"
    }


    @OptIn(ExperimentalTime::class)
    suspend fun etfStockListInfo() {
        val today = LocalDate.now()
        val processorAmova: ProcessingData by inject(named("amova"))
        val processorAsset: ProcessingData by inject(named("asset"))
        val processorGlobalx: ProcessingData by inject(named("globalx"))
        val processorMitsubishi: ProcessingData by inject(named("mitsubishi"))
        val processorNomura: ProcessingData by inject(named("nomura"))
        val processorSimplex: ProcessingData by inject(named("simplex"))
        val etfList = withContext(Dispatchers.IO) {
            transaction {
                EtfList.selectAll().toList()
            }
        }

        coroutineScope {
            //파일 다운로드
            val deferredDownloads: List<Deferred<Unit>> = etfList.map { row ->
                async(Dispatchers.IO) {
                    when {
                        row[EtfList.companyName]!!.startsWith("nomura", true) -> {
                            downloadExcelFile(
                                EtfPublisher.NOMURA.downloadUrl.format(row[EtfList.stockCode]),
                                EtfPublisher.NOMURA.folderName,
                                "${row[EtfList.stockCode]}_data.xlsx"
                            )
                        }

                        row[EtfList.companyName]!!.startsWith("global", true) -> {
                            downloadExcelFile(
                                EtfPublisher.GLOBALX.downloadUrl.format(row[EtfList.stockCode]),
                                EtfPublisher.GLOBALX.folderName,
                                "${row[EtfList.stockCode]}_data.csv"
                            )
                        }

                        row[EtfList.companyName]!!.startsWith("asset", true) -> {
                            downloadExcelFile(
                                EtfPublisher.ASSET.downloadUrl,
                                EtfPublisher.ASSET.folderName,
                                "${row[EtfList.stockCode]}_data.csv"
                            )
                        }

                        row[EtfList.companyName]!!.startsWith("mitsu", true) -> {
                            downloadExcelFile(
                                EtfPublisher.MITSUBISHI.downloadUrl.format(row[EtfList.stockCode]),
                                EtfPublisher.MITSUBISHI.folderName,
                                "${row[EtfList.stockCode]}_data.csv"
                            )
                        }

                        row[EtfList.companyName]!!.startsWith("amova", true) -> {
                            downloadExcelFile(
                                EtfPublisher.AMOVA.downloadUrl.format(
                                    row[EtfList.stockCode], today.format(DateTimeFormatter.ofPattern("yyyyMM"))
                                ),
                                EtfPublisher.AMOVA.folderName,
                                "${row[EtfList.stockCode]}_data.xls"
                            )
                        }

                        row[EtfList.companyName]!!.startsWith("simplex", true) -> {
                            downloadExcelFile(
                                "${EtfPublisher.SIMPLEX.downloadUrl}${today.format(DateTimeFormatter.ofPattern("yyyyMMdd"))}.xlsx",
                                EtfPublisher.SIMPLEX.folderName,
                                "${row[EtfList.stockCode]}_data.xlsx"
                            )
                        }
                    }
                }
            }

            deferredDownloads.awaitAll()

            val dataList = mutableListOf<EtfStockListRecord>()

            // 엑셀파일 읽기
            val nomuraData = readDirectoryToStringList(EtfPublisher.NOMURA.folderName, EtfPublisher.NOMURA.skip)
            val globalData = readDirectoryToStringList(EtfPublisher.GLOBALX.folderName, EtfPublisher.GLOBALX.skip)
            val assetData = readDirectoryToStringList(EtfPublisher.ASSET.folderName, EtfPublisher.ASSET.skip)
            val mitsuData = readDirectoryToStringList(EtfPublisher.MITSUBISHI.folderName, EtfPublisher.MITSUBISHI.skip)
            val amovaData = readDirectoryToStringList(EtfPublisher.AMOVA.folderName, EtfPublisher.AMOVA.skip)
            val simplexData = readDirectoryToStringList(EtfPublisher.SIMPLEX.folderName, EtfPublisher.SIMPLEX.skip)

            dataList.addAll(processorNomura.process(nomuraData))
            dataList.addAll(processorGlobalx.process(globalData))
            dataList.addAll(processorAsset.process(assetData))
            dataList.addAll(processorMitsubishi.process(mitsuData))
            dataList.addAll(processorAmova.process(amovaData))
            dataList.addAll(processorSimplex.process(simplexData))

            // 데이터베이스 작업
            withContext(Dispatchers.IO) {
                transaction {
                    EtfStockList.batchUpsert(dataList) { data ->
                        this[EtfStockList.market] = "TSE"
                        this[EtfStockList.etfStockCode] = data.etfStockCode
                        this[EtfStockList.stockCode] = data.stockCode
                        this[EtfStockList.etfPercent] = data.etfPercent
                        this[EtfStockList.regDate] = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                    }

                    EtfStockList.deleteWhere {
                        EtfStockList.regDate less Clock.System.now()
                            .minus(15, DateTimeUnit.DAY, TimeZone.currentSystemDefault())
                            .toLocalDateTime(TimeZone.currentSystemDefault())
                    }
                }
            }

            //파일 삭제
            removeDirectory(EtfPublisher.NOMURA.folderName)
            removeDirectory(EtfPublisher.GLOBALX.folderName)
            removeDirectory(EtfPublisher.ASSET.folderName)
            removeDirectory(EtfPublisher.MITSUBISHI.folderName)
            removeDirectory(EtfPublisher.AMOVA.folderName)
            removeDirectory(EtfPublisher.SIMPLEX.folderName)
        }
    }

    private suspend fun downloadExcelFile(fileUrl: String, folderName: String, fileNm: String) {
        HttpClient(CIO).use { client ->
            try {
                val response: HttpResponse = client.get(fileUrl)

                if (response.status.isSuccess()) {
                    val bytes = response.body<ByteArray>()
                    val file = File("${excelDir}/${folderName}/${fileNm}")

                    FileOutputStream(file).use { outputStream ->
                        outputStream.write(bytes)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun readDirectoryToStringList(folderName: String, skip: Int): Map<String, List<String>> {
        val directory = File("${excelDir}/${folderName}")

        if (!directory.exists() || !directory.isDirectory) {
            println("유효한 디렉터리 경로가 아닙니다: $directory")
            return emptyMap()
        }

        val filesList = directory.listFiles()?.filter { it.isFile } ?: return emptyMap()

        if (filesList.isEmpty()) {
            return emptyMap()
        }

        return coroutineScope {
            val deferredReads = filesList.map { file ->
                async(Dispatchers.IO) {
                    readDataFile(file, skip)
                }
            }.awaitAll()

            deferredReads.fold(mutableMapOf()) { acc, map ->
                acc.apply { putAll(map) }
            }
        }
    }

    private fun readDataFile(file: File, skip: Int): Map<String, List<String>> {
        val resultList = mutableListOf<String>()

        if (file.isFile && file.exists()) {
            when (file.toPath().extension) {
                "csv" -> {
                    ReaderFactory
                        .createBufferedReader(file, Charset.forName("UTF-8"))
                        .use { reader ->
                            reader.lineSequence()
                                .drop(skip)
                                .forEach { line ->
                                    resultList.add(line)
                                }
                        }
                }

                "xls", "xlsx" -> {
                    val workbook = WorkbookFactory.create(file)
                    workbook.use { workbook ->
                        var rows = workbook.getSheetAt(0)

                        if (rows.lastRowNum < 10 && workbook.numberOfSheets > 1) {
                            rows = workbook.getSheetAt(1)
                        }

                        for (row in rows.drop(skip)) {
                            var blankCellCnt = 0
                            var s = ""

                            for (cell in row) {
                                val cellValue = when (cell.cellType) {
                                    CellType.STRING -> cell.stringCellValue
                                    CellType.NUMERIC -> cell.numericCellValue.toString()
                                    CellType.BOOLEAN -> cell.booleanCellValue.toString()
                                    else -> {
                                        blankCellCnt++
                                        ""
                                    }
                                }
                                s += "${cellValue},"
                            }

                            if (blankCellCnt == row.physicalNumberOfCells) {
                                break
                            }

                            resultList.add(s)
                        }
                    }
                }
            }
        }

        return mapOf(
            file.name.split("_")[0] to resultList
        )
    }

    private fun removeDirectory(folderName: String) {
        val directory = File("${excelDir}/${folderName}")

        if (directory.exists() && directory.isDirectory) {
            directory.listFiles()
                ?.filter { it.isFile }
                ?.forEach { it.delete() }
        }
    }
}