package com.ietf.etfbatch.stock.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class KisPriceDetailResponse(
    @SerialName("rt_cd") val rtCd: String,
    @SerialName("msg_cd") val msgCd: String,
    @SerialName("msg1") val msg1: String?,
    @SerialName("output") val output: KisPriceDetailOutput? = null
)