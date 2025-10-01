package com.ietf.etfbatch.token.dto

import kotlinx.datetime.LocalDateTime

data class KisTokenResponse (val regDate : LocalDateTime) {
    var access_token : String = ""
}