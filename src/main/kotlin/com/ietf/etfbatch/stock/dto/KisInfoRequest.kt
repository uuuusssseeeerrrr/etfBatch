package com.ietf.etfbatch.stock.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class KisInfoRequest(
    @param:JsonProperty("PRDT_TYPE_CD") val marketCode: String,
    @param:JsonProperty("PDNO") val stockCode: String,
)