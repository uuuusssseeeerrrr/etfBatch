package com.ietf.etfbatch.etf.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class InvescoData(
    @SerialName("holdings")
    val dataList: List<InvescoHoldings> = emptyList()
)

@Serializable
data class InvescoHoldings(
    @SerialName("ticker") val ticker: String?,
    @SerialName("percentageOfTotalNetAssets") val percentageOfTotalNetAssets: Double
)