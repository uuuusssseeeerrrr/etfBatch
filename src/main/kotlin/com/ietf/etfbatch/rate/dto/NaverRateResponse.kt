package com.ietf.etfbatch.rate.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NaverRateResponse(
    @SerialName("country")
    var country: List<NaverRateObj>,
)

@Serializable
data class NaverRateObj(
    var value: String,
    var subValue: String,
    var currencyUnit: String
)