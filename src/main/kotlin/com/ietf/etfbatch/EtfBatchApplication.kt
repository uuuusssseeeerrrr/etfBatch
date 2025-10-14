package com.ietf.etfbatch

import com.ietf.etfbatch.config.DataSourceFactory
import com.ietf.etfbatch.config.configureSecurity
import com.ietf.etfbatch.config.koinConfig
import io.ktor.server.application.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureSecurity()
    DataSourceFactory.init()
    configureRouting()
    koinConfig()
}
