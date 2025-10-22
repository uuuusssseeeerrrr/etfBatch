package com.ietf.etfbatch.etf.service

import com.ietf.etfbatch.etf.dto.EtfPublisher
import com.ietf.etfbatch.table.EtfList
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.*
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import kotlin.io.path.extension

class EtfStockListInfoService {
    suspend fun etfStockListInfo() {
        val today = LocalDate.now()
        val etfList = transaction {
            EtfList.selectAll().toList()
        }

        coroutineScope {
//            val deferredDownloads: List<Deferred<Unit>> = etfList.map { row ->
//                async(Dispatchers.IO) {
//                    when {
//                        row[EtfList.companyName]!!.startsWith("nomura", true) -> {
//                            downloadExcelFile(
//                                EtfPublisher.NOMURA.downloadUrl.format(row[EtfList.stockCode]),
//                                EtfPublisher.NOMURA.folderName,
//                                "${row[EtfList.stockCode]}_data.xlsx"
//                            )
//                        }
//
//                        row[EtfList.companyName]!!.startsWith("global", true) -> {
//                            downloadExcelFile(
//                                EtfPublisher.GLOBALX.downloadUrl.format(row[EtfList.stockCode]),
//                                EtfPublisher.GLOBALX.folderName,
//                                "${row[EtfList.stockCode]}_data.csv"
//                            )
//                        }
//
//                        row[EtfList.companyName]!!.startsWith("asset", true) -> {
//                            downloadExcelFile(
//                                EtfPublisher.ASSET.downloadUrl,
//                                EtfPublisher.ASSET.folderName,
//                                "${row[EtfList.stockCode]}_data.csv"
//                            )
//                        }
//
//                        row[EtfList.companyName]!!.startsWith("mitsu", true) -> {
//                            downloadExcelFile(
//                                EtfPublisher.MITSUBISHI.downloadUrl.format(row[EtfList.stockCode]),
//                                EtfPublisher.MITSUBISHI.folderName,
//                                "${row[EtfList.stockCode]}_data.csv"
//                            )
//                        }
//
//                        row[EtfList.companyName]!!.startsWith("amova", true) -> {
//                            downloadExcelFile(
//                                EtfPublisher.AMOVA.downloadUrl.format(
//                                    row[EtfList.stockCode], today.format(DateTimeFormatter.ofPattern("yyyyMM"))
//                                ),
//                                EtfPublisher.AMOVA.folderName,
//                                "${row[EtfList.stockCode]}_data.xls"
//                            )
//                        }

//                        row[EtfList.companyName]!!.startsWith("simplex", true) -> {
//                            downloadExcelFile(
//                                "${EtfPublisher.SIMPLEX.downloadUrl}${today.format(DateTimeFormatter.ofPattern("yyyyMMdd"))}.xlsx",
//                                EtfPublisher.SIMPLEX.folderName,
//                                "${row[EtfList.stockCode]}_data.xlsx"
//                            )
//                        }
//                    }
//                }
//            }

//            deferredDownloads.awaitAll()

            val nomuraData = readDirectoryToStringList(EtfPublisher.NOMURA.folderName, EtfPublisher.NOMURA.skip)
//            val globalData = readDirectoryToStringList(EtfPublisher.GLOBALX.folderName, EtfPublisher.GLOBALX.skip)
//            val assetData = readDirectoryToStringList(EtfPublisher.ASSET.folderName, EtfPublisher.ASSET.skip)
//            val mitsuData = readDirectoryToStringList(EtfPublisher.MITSUBISHI.folderName, EtfPublisher.MITSUBISHI.skip)
//            val amovaData = readDirectoryToStringList(EtfPublisher.AMOVA.folderName, EtfPublisher.AMOVA.skip)
//            val simplexData = readDirectoryToStringList(EtfPublisher.SIMPLEX.folderName, EtfPublisher.SIMPLEX.skip)

            nomuraData.forEach { println(it) }
//            globalData.forEach { println(it) }
//            assetData.forEach { println(it) }
//            mitsuData.forEach { println(it) }
//            amovaData.forEach { println(it) }
//            simplexData.forEach { println(it) }
        }
    }

    private suspend fun downloadExcelFile(fileUrl: String, folderName: String, fileNm: String) {
        HttpClient(CIO).use { client ->
            try {
                val response: HttpResponse = client.get(fileUrl)

                if (response.status.isSuccess()) {
                    // 응답 본문을 바이트 배열로 받습니다.
                    val bytes = response.body<ByteArray>()

                    // 바이트를 파일에 씁니다. (IO 작업은 Dispatchers.IO에서 실행)
                    withContext(Dispatchers.IO) {
                        val file = File("${System.getProperty("user.home")}/excel/${folderName}/${fileNm}")
                        FileOutputStream(file).use { outputStream ->
                            outputStream.write(bytes)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun readDirectoryToStringList(folderName: String, skip: Int): List<String> {
        val directory = File("${System.getProperty("user.home")}/excel/${folderName}")

        if (!directory.exists() || !directory.isDirectory) {
            println("유효한 디렉터리 경로가 아닙니다: $directory")
            return emptyList()
        }

        val filesList = directory.listFiles()
            ?.filter { it.isFile }
            ?.toList()
            ?: emptyList()

        if (filesList.isEmpty()) {
            return emptyList()
        }

        return coroutineScope {
            val deferredReads: List<Deferred<List<String>>> = filesList.map { file ->
                async(Dispatchers.IO) {
                    readDataFile(file, skip)
                }
            }

            val listOfLists: List<List<String>> = deferredReads.awaitAll()
            listOfLists.flatten()
        }
    }

    private fun readDataFile(file: File, skip: Int): List<String> {
        val resultList = mutableListOf<String>()

        if (file.isFile && file.exists()) {
            when (file.toPath().extension) {
                "csv" -> {
                    file.bufferedReader().use { reader ->
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

                        if(rows.lastRowNum < 10 && workbook.numberOfSheets > 1) {
                            rows = workbook.getSheetAt(1)
                        }

                        for (row in rows.drop(skip)) {
                            var blankCellCnt = 0;
                            var s = ""

                            for (cell in row) {
                                val cellValue = when (cell.cellType) {
                                    org.apache.poi.ss.usermodel.CellType.STRING -> cell.stringCellValue
                                    org.apache.poi.ss.usermodel.CellType.NUMERIC -> cell.numericCellValue.toString()
                                    org.apache.poi.ss.usermodel.CellType.BOOLEAN -> cell.booleanCellValue.toString()
                                    else -> {
                                        blankCellCnt++
                                        ""
                                    }
                                }
                                s += "${cellValue},"
                            }

                            if(blankCellCnt == row.physicalNumberOfCells) {
                                break
                            }

                            resultList.add(s)
                        }
                    }
                }
            }
        }

        return resultList
    }
}