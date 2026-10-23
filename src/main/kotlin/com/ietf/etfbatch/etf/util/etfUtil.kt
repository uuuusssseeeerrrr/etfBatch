package com.ietf.etfbatch.etf.util

fun removeCharEtfName(etfName: String): String {
    val regexString = ",\\s*(LT|IN|LIM)"
    return etfName
        .replace(regexString.toRegex(RegexOption.IGNORE_CASE), "")
        .replace("CO.,", "")
        .replace("E,", "")
}

/**
 * 실패한 작업이 있으면 첫 번째 예외에 나머지를 suppressed로 묶어 던집니다.
 */
fun throwIfFailed(failures: List<Throwable>) {
    if (failures.isEmpty()) return

    val first = failures.first()
    failures.drop(1).forEach { first.addSuppressed(it) }
    throw first
}
