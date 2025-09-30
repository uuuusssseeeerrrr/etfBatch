package com.ietf.etfbatch.token.model

import kotlinx.datetime.LocalDateTime

data class KisTokenResponse (val regDate : LocalDateTime) {
    var access_token : String = ""
}