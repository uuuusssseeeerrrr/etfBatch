package com.ietf.etfbatch.config

import io.ktor.server.application.*
import io.ktor.server.auth.*

fun Application.configureSecurity() {
    val token = environment.config.property("custom.batchToken").getString()

    install(Authentication) {
        bearer("tokenAuth") {
            authenticate { tokenCredential ->
                if (tokenCredential.token == token) {
                    "ok"
                } else {
                    null
                }
            }
        }
    }
}