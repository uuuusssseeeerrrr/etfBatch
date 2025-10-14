package com.ietf.etfbatch.stock.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class KisInfoOutput(
    var market: String?,
    var stockCode: String?,
    @SerialName("std_pdno")
    val stdPdno: String,
    @SerialName("tr_crcy_cd")
    val trCrcyCd: String,
    @SerialName("buy_unit_qty")
    val buyUnitQty: String,
    @SerialName("prdt_name")
    val prdtName: String
) 