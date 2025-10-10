package com.ietf.etfbatch.token.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class KisTokenResponse(
    @param:JsonProperty("access_token")
    val accessToken: String,

    @param:JsonProperty("token_type")
    val tokenType: String,

    @param:JsonProperty("expires_in")
    val expiresIn: Int,

    @param:JsonProperty("access_token_token_expired")
    val accessTokenTokenExpired: String
)