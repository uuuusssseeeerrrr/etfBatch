package com.ietf.etfbatch.etf.service

fun removeCharEtfName(etfName: String): String {
    val regexString = ",\\s*(LT|IN|LIM)"
    return etfName
        .replace(regexString.toRegex(RegexOption.IGNORE_CASE), "")
        .replace("CO.,", "")
        .replace("E,", "")
}