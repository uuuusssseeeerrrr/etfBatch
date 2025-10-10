package com.ietf.etfbatch.stock.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class KisInfoOutput(
    var market: String?,
    var stockCode: String?,
    @param:JsonProperty("std_pdno")
    val stdPdno: String,
    @param:JsonProperty("tr_crcy_cd")
    val trCrcyCd: String,
    @param:JsonProperty("buy_unit_qty")
    val buyUnitQty: String,
    @param:JsonProperty("prdt_name")
    val prdtName: String
) 