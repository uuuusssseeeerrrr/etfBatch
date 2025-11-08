package com.ietf.etfbatch.stock.enum

enum class PrdtTypeCd(val marketCode: String, val marketCodeDay: String, val infoCode: Int) {
    HONGKONG("HKS", "HKS", 501),
    USA_NEWYORK("NYS", "BAY", 513),
    USA_NASDAQ("NAS", "BAQ", 512),
    USA_AMEX("AMS", "BAA", 529),
    JAPAN("TSE", "TSE", 515),
    CHINA_SHANGHAI("SHS", "SHS", 551),
    CHINA_SHENZHEN("SZS", "SZS", 552),
    VIETNAM_HOCHIMINH("HSX", "HSX", 508),
    VIETNAM_HANOI("HNX", "HNX", 507);

    companion object {
        fun findInfoCodeByMarketCode(marketCode: String): Int {
            return entries.find { it.marketCode == marketCode }?.infoCode ?: 0
        }

        fun findDayMarketCodeByMarketCode(marketCode: String): String {
            return entries.find { it.marketCode == marketCode }?.marketCodeDay ?: marketCode
        }
    }
}
