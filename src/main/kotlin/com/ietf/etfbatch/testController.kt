package com.ietf.etfbatch

import com.ietf.etfbatch.stock.service.KisStockService
import com.ietf.etfbatch.token.service.KisTokenService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class testController (val kisTokenService: KisTokenService,
    val kisStockService: KisStockService) {
    @GetMapping("/tokenT")
    fun token(): String {
        kisTokenService.getKisAccessToken()

        return "GET 요청 처리됨"
    }

    @GetMapping("/etf")
    fun etf(): String {
        kisStockService.getEtfPrice()
        return "etf 요청 처리됨"
    }
}