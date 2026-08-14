package com.ietf.etfbatch.etf.service

import com.ietf.etfbatch.etf.service.interfaces.SyncData
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

@Single
class EtfStocksSyncService(
    @Named("usa") val usaEtfStockSyncService: SyncData,
    @Named("japan") val japanEtfStockSyncService: SyncData,
) {
    suspend fun etfStockListInfo() {
        japanEtfStockSyncService.sync()
        usaEtfStockSyncService.sync()
    }
}