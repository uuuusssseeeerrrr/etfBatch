package com.ietf.etfbatch.etf.dto

enum class EtfPublisher(
    val folderName: String,
    val downloadUrl: String,
    val skip: Int
) {
    NOMURA(
        "nomura",
        "https://www.nomura-am.co.jp/fund/monthly_holdings/%s_brd_data.xlsx",
        3
    ),
    GLOBALX(
        "globalx",
        "https://www.solactive.com/downloads/etfservices/tse-pcf/single/%s.csv",
        4
    ),
    ASSET(
        "asset",
        "https://www.am-one.co.jp/fund/csv/313008/313008_pcf.csv",
        20
    ),
    MITSUBISHI(
        "mitsubishi",
        "https://maxis.am.mufg.jp/fund_file/english/kumiire/18%s.csv",
        15
    ),
    AMOVA(
        "amova",
        "https://global.amova-am.com/docs/default-source/jp-library/funds/portfolio/%s_PORTFOLIO.xls",
        14
    ),
    SIMPLEX(
        "simplex",
        "https://www.simplexasset.com/etf/Doc/WEB_%E8%A8%AD%E5%AE%9A%E8%A7%A3%E7%B4%84PF",
        2
    );
}