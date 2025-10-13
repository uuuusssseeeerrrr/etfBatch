package com.ietf.etfbatch

import com.ietf.etfbatch.config.configureSecurity
import com.ietf.etfbatch.config.dataSourceFactory
import com.ietf.etfbatch.config.jsonConfig
import com.ietf.etfbatch.config.koinConfig
import io.ktor.server.application.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureSecurity()
    dataSourceFactory.init()
    jsonConfig()
    configureRouting()
    koinConfig()
}
