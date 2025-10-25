package com.ietf.etfbatch.etf.service

import com.ietf.etfbatch.table.EtfStockListRecord

interface ProcessingData {
    suspend fun process(data: Map<String, List<String>>): List<EtfStockListRecord>
}