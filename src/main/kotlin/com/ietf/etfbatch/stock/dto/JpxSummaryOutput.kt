package com.ietf.etfbatch.stock.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class JpxSummaryOutput(
    @SerialName("FLLN") val flln: String? = null,
    @SerialName("FLLNE") val fllne: String? = null,
    @SerialName("TTCODE") val ttCode: String? = null,
    @SerialName("TTCODE2") val ttCode2: String? = null,
    @SerialName("SUMMARY") val summary: String? = null
)
