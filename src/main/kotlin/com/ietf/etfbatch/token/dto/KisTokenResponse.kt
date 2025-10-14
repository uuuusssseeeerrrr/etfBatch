package com.ietf.etfbatch.token.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class KisTokenResponse(
    @SerialName("access_token")
    val accessToken: String? = null,

    @SerialName("token_type")
    val tokenType: String? = null,

    @SerialName("expires_in")
    val expiresIn: Int = 0,

    @SerialName("access_token_token_expired")
    val accessTokenTokenExpired: String? = null,

    @SerialName("error_description")
    val errorDescription: String? = null,

    @SerialName("error_code")
    val errorCode: String? = null
)