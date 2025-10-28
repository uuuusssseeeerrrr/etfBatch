package com.ietf.etfbatch.config

import com.ietf.etfbatch.etf.service.EtfFileHandler
import com.ietf.etfbatch.etf.service.EtfStocksSyncService
import com.ietf.etfbatch.etf.service.ProcessData
import com.ietf.etfbatch.etf.service.StockListSyncService
import com.ietf.etfbatch.etf.service.impl.*
import com.ietf.etfbatch.rate.service.RateService
import com.ietf.etfbatch.stock.service.KisStockInfoService
import com.ietf.etfbatch.stock.service.KisStockPriceService
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
        singleOf(::KisStockInfoService)
        singleOf(::KisStockPriceService)
        singleOf(::StockRemoveService)
        singleOf(::RateService)
        singleOf(::StockListSyncService)
        singleOf(::EtfFileHandler)

        single {
            EtfStocksSyncService(
                etfFileHandler = get(),
                processorAmova = get(named("amova")),
                processorAsset = get(named("asset")),
                processorGlobalx = get(named("globalx")),
                processorMitsubishi = get(named("mitsubishi")),
                processorNomura = get(named("nomura")),
                processorSimplex = get(named("simplex"))
            )
        }


        single<ProcessData>(named("amova")) { ProcessAmovaData() }
        single<ProcessData>(named("asset")) { ProcessAssetData() }
        single<ProcessData>(named("globalx")) { ProcessGlobalXData() }
        single<ProcessData>(named("mitsubishi")) { ProcessMitsubishiData() }
        single<ProcessData>(named("nomura")) { ProcessNomuraData() }
        single<ProcessData>(named("simplex")) { ProcessSimplexData() }
    }

    install(Koin) {
        slf4jLogger()
        modules(restClient)
        modules(clientModule)
    }
}