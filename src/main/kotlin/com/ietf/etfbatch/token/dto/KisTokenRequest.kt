package com.ietf.etfbatch.token.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class KisTokenRequest(
    @field:JsonProperty("appkey")
    val appKey: String,

    @field:JsonProperty("appsecret")
    val appSecret: String
) {
    @JsonProperty("grant_type")
    val grantType = "client_credentials"
}