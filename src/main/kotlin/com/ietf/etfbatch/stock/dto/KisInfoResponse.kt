package com.ietf.etfbatch.stock.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class KisInfoResponse(
    @param:JsonProperty("rt_cd") val rtCd: String,
    @param:JsonProperty("msg_cd") val msgCd: String,
    @param:JsonProperty("msg1") val msg1: String?,
    @param:JsonProperty("output") val output: KisInfoOutput? = null
)