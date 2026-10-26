package com.ietf.etfbatch.stock.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class JpxSummaryResponse(
    @SerialName("cputime") val cpuTime: String? = null,
    @SerialName("section1") val section1: JpxSummarySection? = null,
    @SerialName("status") val status: Int,
    @SerialName("ver") val ver: String? = null
)

@Serializable
data class JpxSummarySection(
    // key: 종목코드 (예: "1376"), 조회 결과가 없으면(hitcount 0) null
    @SerialName("data") val data: Map<String, JpxSummaryOutput>? = null,
    @SerialName("hitcount") val hitCount: Int,
    @SerialName("status") val status: Int,
    @SerialName("type") val type: String? = null
)
