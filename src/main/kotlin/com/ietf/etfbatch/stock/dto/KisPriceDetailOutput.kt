package com.ietf.etfbatch.stock.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class KisPriceDetailOutput(
    var market: String?,
    var stockCode: String?,
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
    @param:JsonProperty("t_xprc")
    val tXprc: String,
    @param:JsonProperty("t_xdif")
    val tXdif: String,
    @param:JsonProperty("t_xrat")
    val tXrat: String,
    @param:JsonProperty("t_rate")
    val tRate: String,
    @param:JsonProperty("t_xsgn")
    val tXsgn: String,
    @param:JsonProperty("e_ordyn")
    val eOrdyn: String,
    @param:JsonProperty("e_hogau")
    val eHogau: String,
    @param:JsonProperty("e_icod")
    val eIcod: String,
    @param:JsonProperty("e_parp")
    val eParp: String,
    val tvol: String,
    val tamt: String,
    @param:JsonProperty("etyp_nm")
    val etypNm: String
) 