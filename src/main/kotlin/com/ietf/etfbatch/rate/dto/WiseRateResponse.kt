package com.ietf.etfbatch.rate.dto

import kotlinx.serialization.Serializable

@Serializable
data class WiseRateResponse(
    var rate: Double,
    var source: String,
    var target: String
)