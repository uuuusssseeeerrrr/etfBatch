package com.ietf.etfbatch.token.dto

import kotlinx.serialization.SerialName

data class KisTokenRequest(
    @SerialName("appkey")
    val appKey: String,

    @SerialName("appsecret")
    val appSecret: String
) {
    @SerialName("grant_type")
    val grantType = "client_credentials"
}