package com.ietf.etfbatch.token.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class KisTokenRequest(
    @SerialName("appkey")
    val appKey: String,

    @SerialName("appsecret")
    val appSecret: String,

    @SerialName("grant_type")
    val grantType: String = "client_credentials"
)
