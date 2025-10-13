package com.ietf.etfbatch.config

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.bearer

fun Application.configureSecurity() {
    val token = environment.config.property("custom.batchToken").getString()

    install(Authentication) {
        bearer {
            authenticate { tokenCredential ->
                if (tokenCredential.token == token) {
                    UserIdPrincipal("auth")
                } else {
                    null
                }
            }
        }
    }
}