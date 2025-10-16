package com.ietf.etfbatch.config

import com.ietf.etfbatch.stock.service.KisInfoService
import com.ietf.etfbatch.stock.service.KisStockService
import com.ietf.etfbatch.stock.service.StockRemoveService
import com.ietf.etfbatch.token.service.KisTokenService
import io.ktor.server.application.*
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun Application.koinConfig() {
    val clientModule = module {
        singleOf(::KisTokenService)
        singleOf(::KisInfoService)
        singleOf(::KisStockService)
        singleOf(::StockRemoveService)
    }

    install(Koin) {
        slf4jLogger()
        modules(restClient)
        modules(clientModule)
    }
}