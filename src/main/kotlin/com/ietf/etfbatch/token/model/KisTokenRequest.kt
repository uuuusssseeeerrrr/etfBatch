package com.ietf.etfbatch.token.model

data class KisTokenRequest(val grant_type: String = "client_credentials") {
    lateinit var appkey: String
    lateinit var appsecret: String
}