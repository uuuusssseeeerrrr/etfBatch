package com.ietf.etfbatch.`interface`

import com.ietf.etfbatch.token.model.KisTokenRequest
import com.ietf.etfbatch.token.model.KisTokenResponse
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.service.annotation.PostExchange

interface KisInterface {
    @PostExchange("/oauth2/tokenP", contentType = "application/json; charset=utf-8")
    fun tokenApiCall(@RequestBody request: KisTokenRequest): KisTokenResponse

    @PostExchange("/uapi/overseas-price/v1/quotations/price-detail", contentType = "application/json; charset=utf-8")
    fun priceDetailApiCall(
        @RequestHeader("tr_id") trId: String,
        @RequestHeader("custtype") custtype: String,
        @RequestBody request: KisTokenRequest
    ): KisTokenResponse

    @PostExchange("/uapi/overseas-price/v1/quotations/search-info", contentType = "application/json; charset=utf-8")
    fun searchInfoApiCall(
        @RequestHeader("tr_id") trId: String,
        @RequestHeader("custtype") custtype: String,
        @RequestBody request: KisTokenRequest
    ): KisTokenResponse
}