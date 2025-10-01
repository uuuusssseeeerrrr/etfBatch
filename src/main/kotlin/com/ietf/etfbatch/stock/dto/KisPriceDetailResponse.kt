package com.ietf.etfbatch.stock.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class KisPriceDetailResponse (@JsonProperty("rt_cd") val rtCd: String,
                                   @JsonProperty("msg_cd") val msg: String,
                                   @JsonProperty("msg1") val msg1: String?,
                                   @JsonProperty("output") val output: KisPriceDetailOutput) {
}