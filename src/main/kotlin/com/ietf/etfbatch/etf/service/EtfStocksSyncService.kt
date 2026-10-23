package com.ietf.etfbatch.etf.service

import com.ietf.etfbatch.etf.service.interfaces.SyncData
import com.ietf.etfbatch.etf.util.throwIfFailed
import kotlinx.coroutines.CancellationException
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single
import org.slf4j.LoggerFactory

@Single
class EtfStocksSyncService(
    @Named("usa") val usaEtfStockSyncService: SyncData,
    @Named("japan") val japanEtfStockSyncService: SyncData,
) {
    companion object {
        private val logger = LoggerFactory.getLogger(EtfStocksSyncService::class.java)
    }

    /**
     * 일본/미국 ETF 비중정보를 동기화합니다.
     * 한쪽이 실패해도 다른 쪽은 계속 진행하고, 실패가 있으면 마지막에 예외를 던집니다.
     */
    suspend fun etfStockListInfo() {
        val failures = listOf(
            "일본" to japanEtfStockSyncService,
            "미국" to usaEtfStockSyncService
        ).mapNotNull { (market, service) ->
            try {
                service.sync()
                null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.error("$market ETF 비중정보 동기화 중 오류 발생", e)
                e
            }
        }

        throwIfFailed(failures)
    }
}
