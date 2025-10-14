package com.ietf.etfbatch.stock.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class KisPriceDetailOutput(
    var market: String? = null,
    var stockCode: String? = null,
    val rsym: String,
    val zdiv: String,
    val curr: String,
    val vnit: String,
    val open: String,
    val high: String,
    val low: String,
    val last: String,
    val base: String,
    val pvol: String,
    val pamt: String,
    val uplp: String,
    val dnlp: String,
    val h52p: String,
    val h52d: String,
    val l52p: String,
    val l52d: String,
    val perx: String,
    val pbrx: String,
    val epsx: String,
    val bpsx: String,
    val shar: String,
    val mcap: String,
    val tomv: String,
    @SerialName("t_xprc")
    val tXprc: String,
    @SerialName("t_xdif")
    val tXdif: String,
    @SerialName("t_xrat")
    val tXrat: String,
    @SerialName("t_rate")
    val tRate: String,
    @SerialName("t_xsgn")
    val tXsgn: String,
    @SerialName("e_ordyn")
    val eOrdyn: String,
    @SerialName("e_hogau")
    val eHogau: String,
    @SerialName("e_icod")
    val eIcod: String,
    @SerialName("e_parp")
    val eParp: String,
    val tvol: String,
    val tamt: String,
    @SerialName("etyp_nm")
    val etypNm: String
) 