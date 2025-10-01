package com.ietf.etfbatch.httpInf

import com.ietf.etfbatch.stock.dto.KisPriceDetailResponse
import com.ietf.etfbatch.token.dto.KisTokenRequest
import com.ietf.etfbatch.token.dto.KisTokenResponse
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.service.annotation.GetExchange
import org.springframework.web.service.annotation.HttpExchange
import org.springframework.web.service.annotation.PostExchange

@HttpExchange
interface KisInterface {
    @PostExchange("/oauth2/tokenP")
    fun tokenApiCall(@RequestBody request: KisTokenRequest): KisTokenResponse

    @GetExchange("/uapi/overseas-price/v1/quotations/price-detail")
    fun priceDetailApiCall(
        @RequestHeader("tr_id") trId: String,
        @RequestHeader("authorization") authorization: String,
        @RequestParam("EXCD") EXCD: String,
        @RequestParam("SYMB") SYMB: String,
        @RequestParam("AUTH", required = false) AUTH: String = ""
    ): KisPriceDetailResponse

    @PostExchange("/uapi/overseas-price/v1/quotations/search-info", contentType = "application/json; charset=utf-8")
    fun searchInfoApiCall(
        @RequestHeader("tr_id") trId: String,
        @RequestHeader("custtype") custtype: String,
        @RequestBody request: KisTokenRequest
    ): KisTokenResponse
}