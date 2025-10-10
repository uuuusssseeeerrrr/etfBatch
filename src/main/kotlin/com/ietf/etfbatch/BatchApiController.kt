package com.ietf.etfbatch

import com.ietf.etfbatch.stock.service.KisInfoService
import com.ietf.etfbatch.stock.service.KisStockService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class BatchApiController(
    val kisInfoService: KisInfoService,
    val kisStockService: KisStockService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(KisStockService::class.java)
    }

    @GetMapping("/etf")
    fun etf(): ResponseEntity<String> {
        try {
            kisStockService.getEtfPrice()
        } catch (e: Exception) {
            logger.error(e.message, e)
            return ResponseEntity.internalServerError().build()
        }

        return ResponseEntity.ok("etf 요청 처리됨")
    }

    @GetMapping("/stock")
    fun stock(): ResponseEntity<String> {
        try {
            kisStockService.getStockPrice()
        } catch (e: Exception) {
            logger.error(e.message, e)
            return ResponseEntity.internalServerError().build()
        }

        return ResponseEntity.ok("stock 요청 처리됨")
    }

    @GetMapping("/kisInfo")
    fun kisInfo(): ResponseEntity<String> {
        try {
            kisInfoService.kisInfo()
        } catch (e: Exception) {
            logger.error(e.message, e)
            return ResponseEntity.internalServerError().build()
        }

        return ResponseEntity.ok("kisInfo 요청 처리됨")
    }
}