package com.ietf.etfbatch

import com.ietf.etfbatch.config.DataSourceFactory
import com.ietf.etfbatch.config.configureSecurity
import com.ietf.etfbatch.config.koinConfig
import io.ktor.server.application.*
import org.koin.ktor.ext.get

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    koinConfig()
    configureSecurity()
    DataSourceFactory.init()
    get<RoutingClass>().configureRouting(this)
}
