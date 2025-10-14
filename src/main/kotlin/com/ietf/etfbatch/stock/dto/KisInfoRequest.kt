package com.ietf.etfbatch.stock.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class KisInfoRequest(
    @SerialName("PRDT_TYPE_CD") val marketCode: String,
    @SerialName("PDNO") val stockCode: String,
)