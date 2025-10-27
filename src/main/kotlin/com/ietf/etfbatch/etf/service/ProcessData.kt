package com.ietf.etfbatch.etf.service

import com.ietf.etfbatch.table.EtfStockListRecord

interface ProcessData {
    suspend fun processData(data: Map<String, List<String>>): List<EtfStockListRecord>
}