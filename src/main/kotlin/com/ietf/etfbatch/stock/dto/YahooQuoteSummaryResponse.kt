package com.ietf.etfbatch.stock.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class YahooQuoteSummaryResponse(
    @SerialName("quoteSummary") val quoteSummary: YahooQuoteSummary? = null
)

@Serializable
data class YahooQuoteSummary(
    @SerialName("result") val result: List<YahooQuoteSummaryResult>? = null,
    @SerialName("error") val error: YahooQuoteSummaryError? = null
)

@Serializable
data class YahooQuoteSummaryResult(
    @SerialName("assetProfile") val assetProfile: YahooAssetProfile? = null
)

@Serializable
data class YahooAssetProfile(
    @SerialName("longBusinessSummary") val longBusinessSummary: String? = null
)

@Serializable
data class YahooQuoteSummaryError(
    // 예: "Unauthorized" (crumb 만료), "Not Found" (없는 종목)
    @SerialName("code") val code: String? = null,
    @SerialName("description") val description: String? = null
)
