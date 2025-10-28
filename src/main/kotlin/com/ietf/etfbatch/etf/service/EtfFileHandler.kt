package com.ietf.etfbatch.etf.service

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.mozilla.universalchardet.ReaderFactory
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.Charset
import kotlin.io.path.extension

class EtfFileHandler(val client: HttpClient) {
    private val excelDir: String = System.getenv("EXCEL_DIR") ?: "/home/rocky/excel"

    suspend fun downloadFileToExcelDir(fileUrl: String, folderName: String, fileNm: String) {
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

    suspend fun readDirectoryToStringList(folderName: String, skip: Int): Map<String, List<String>> {
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

    fun readDataFile(file: File, skip: Int): Map<String, List<String>> {
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

                //factory가 정상적으로 xls 못읽어서 직접읽음
                "xls" -> {
                    file.inputStream().use { stream ->
                        HSSFWorkbook(stream).use { workbook ->
                            resultList.addAll(readExcelData(workbook, skip))
                        }
                    }
                }

                "xlsx" -> {
                    file.inputStream().use { stream ->
                        XSSFWorkbook(stream).use { workbook ->
                            resultList.addAll(readExcelData(workbook, skip))
                        }
                    }
                }
            }
        }

        return mapOf(
            file.name.split("_")[0] to resultList
        )
    }

    fun removeDirectory(folderName: String) {
        val directory = File("${excelDir}/${folderName}")

        if (directory.exists() && directory.isDirectory) {
            directory.listFiles()
                ?.filter { it.isFile }
                ?.forEach { it.delete() }
        }
    }

    fun readExcelData(workbook: Workbook, skip: Int): List<String> {
        val resultList = mutableListOf<String>()
        var rows = workbook.getSheetAt(0)

        if (rows.lastRowNum < 10 && workbook.numberOfSheets > 1) {
            rows = workbook.getSheetAt(1)
        }

        for (row in rows.drop(skip)) {
            var blankCellCnt = 0
            val s = StringBuilder()

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
                s.append(cellValue).append(",")
            }

            if (blankCellCnt == row.physicalNumberOfCells) {
                break
            }

            resultList.add(s.toString())
        }

        return resultList.toList()
    }
}