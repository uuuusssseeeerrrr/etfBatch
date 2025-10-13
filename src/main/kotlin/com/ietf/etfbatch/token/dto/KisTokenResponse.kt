package com.ietf.etfbatch.token.dto

import kotlinx.serialization.SerialName

data class KisTokenResponse(
    @SerialName("access_token")
    val accessToken: String,

    @SerialName("token_type")
    val tokenType: String,

    @SerialName("expires_in")
    val expiresIn: Int,

    @SerialName("access_token_token_expired")
    val accessTokenTokenExpired: String
)