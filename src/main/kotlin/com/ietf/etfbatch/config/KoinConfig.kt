package com.ietf.etfbatch.config

import com.ietf.etfbatch.etf.service.EtfStockListInfoService
import com.ietf.etfbatch.etf.service.ProcessingData
import com.ietf.etfbatch.etf.service.impl.*
import com.ietf.etfbatch.rate.service.RateService
import com.ietf.etfbatch.stock.service.KisInfoService
import com.ietf.etfbatch.stock.service.KisStockService
import com.ietf.etfbatch.stock.service.StockRemoveService
import com.ietf.etfbatch.token.service.KisTokenService
import io.ktor.server.application.*
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun Application.koinConfig() {
    val clientModule = module {
        singleOf(::KisTokenService)
        singleOf(::KisInfoService)
        singleOf(::KisStockService)
        singleOf(::StockRemoveService)
        singleOf(::RateService)
        singleOf(::EtfStockListInfoService)
        single<ProcessingData>(named("amova")) { ProcessingDataAmova() }
        single<ProcessingData>(named("asset")) { ProcessingDataAsset() }
        single<ProcessingData>(named("globalx")) { ProcessingDataGlobalX() }
        single<ProcessingData>(named("mitsubishi")) { ProcessingDataMitsubishi() }
        single<ProcessingData>(named("nomura")) { ProcessingDataNomura() }
        single<ProcessingData>(named("simplex")) { ProcessingDataSimplex() }
    }

    install(Koin) {
        slf4jLogger()
        modules(restClient)
        modules(clientModule)
    }
}