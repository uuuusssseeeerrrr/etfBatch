package com.ietf.etfbatch.token.dto

data class KisTokenResponse(
    val access_token: String,
    val token_type: String,
    val expires_in: Int,
    val access_token_token_expired: String
)