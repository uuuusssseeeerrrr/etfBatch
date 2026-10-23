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

    // 실제 조회에 성공한 시장코드 (재조회로 시장이 바뀐 경우 market과 다름)
    @Transient
    lateinit var resolvedMarket: String
}