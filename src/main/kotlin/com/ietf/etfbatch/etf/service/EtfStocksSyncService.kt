package com.ietf.etfbatch.etf.service

import com.ietf.etfbatch.etf.service.interfaces.SyncData
import jakarta.inject.Named

class EtfStocksSyncService(
    @param:Named("usa") val usaEtfStockSyncService: SyncData,
    @param:Named("japan") val japanEtfStockSyncService: SyncData,
) {
    suspend fun etfStockListInfo() {
        japanEtfStockSyncService.sync()
        usaEtfStockSyncService.sync()
    }
}