package com.ietf.etfbatch.token.dto

data class KisTokenRequest(val grant_type: String = "client_credentials") {
    lateinit var appkey: String
    lateinit var appsecret: String
}