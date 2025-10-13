package com.ietf.etfbatch.stock.dto

import kotlinx.serialization.SerialName

data class KisInfoResponse(
    @SerialName("rt_cd") val rtCd: String,
    @SerialName("msg_cd") val msgCd: String,
    @SerialName("msg1") val msg1: String?,
    @SerialName("output") val output: KisInfoOutput? = null
)