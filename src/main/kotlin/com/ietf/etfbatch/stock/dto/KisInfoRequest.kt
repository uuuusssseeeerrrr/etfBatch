package com.ietf.etfbatch.stock.dto

import kotlinx.serialization.SerialName

data class KisInfoRequest(
    @SerialName("PRDT_TYPE_CD") val marketCode: String,
    @SerialName("PDNO") val stockCode: String,
)