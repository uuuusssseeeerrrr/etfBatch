package com.ietf.etfbatch.token.dto

data class KisTokenRequest(
    val appkey: String,
    val appsecret: String
) {
    val grant_type = "client_credentials"
}