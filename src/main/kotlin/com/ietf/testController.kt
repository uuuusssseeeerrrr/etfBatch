package com.ietf

import com.ietf.etfbatch.token.service.KisTokenService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class testController (val kisTokenService: KisTokenService) {
    @GetMapping("/token")
    fun token(): String {
        kisTokenService.getKisAccessToken()

        return "GET 요청 처리됨"
    }
}