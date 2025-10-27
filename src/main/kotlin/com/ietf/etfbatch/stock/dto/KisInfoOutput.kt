package com.ietf.etfbatch.stock.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class KisInfoOutput(
    @SerialName("std_pdno")
    val stdPdno: String,
    @SerialName("tr_crcy_cd")
    val trCrcyCd: String,
    @SerialName("buy_unit_qty")
    val buyUnitQty: String,
    @SerialName("prdt_name")
    val prdtName: String,
    @SerialName("istt_usge_isin_cd")
    val isinCd: String,
    @SerialName("prdt_eng_name")
    val prdtEngName: String
) {
    @Transient
    lateinit var market: String

    @Transient
    lateinit var stockCode: String
}