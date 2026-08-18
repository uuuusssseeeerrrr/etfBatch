package com.ietf.etfbatch.holiday.service

import com.ietf.etfbatch.holiday.dto.CalendarificResponse
import com.ietf.etfbatch.holiday.dto.HolidayDto
import com.ietf.etfbatch.table.Holiday
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.batchUpsert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.koin.core.annotation.Single
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.ZoneId
import kotlin.time.Clock

@Single
class HolidayService(private val httpClient: HttpClient) {

    companion object {
        private val logger = LoggerFactory.getLogger(HolidayService::class.java)
        private const val BASE_URL = "https://calendarific.com/api/v2/holidays"
        const val DEFAULT_COUNTRY = "JP"

        // DB에 저장할 대상 primary_type 목록 (소문자 기준)
        val TARGET_PRIMARY_TYPES = setOf("national holiday", "bank holiday")

        /**
         * 한국/일본 시간(Asia/Tokyo) 기준의 현재 4자리 연도를 반환합니다.
         */
        fun getCurrentYear(): Int = LocalDate.now(ZoneId.of("Asia/Tokyo")).year
    }

    /**
     * Calendarific API를 호출하여 전체 휴일 목록을 조회합니다.
     *
     * @param country 국가 코드 (기본값: "JP")
     * @param year 연도 (기본값: 현재 시간 기준 4자리 연도, e.g. 2026)
     * @return List<HolidayDto> 휴일 목록
     */
    suspend fun getHolidays(
        country: String = DEFAULT_COUNTRY,
        year: Int = getCurrentYear()
    ): List<HolidayDto> {
        val apiKey = System.getenv("CALENDARIFIC_API_KEY") ?: ""
        if (apiKey.isBlank()) {
            logger.error("Calendarific API 키가 설정되지 않았습니다. OS 환경 변수(CALENDARIFIC_API_KEY)를 설정해주세요.")
            throw IllegalStateException("Calendarific API key is missing. Please set the CALENDARIFIC_API_KEY environment variable.")
        }

        return try {
            logger.info("Calendarific 휴일 조회 요청 시작 - country: {}, year: {}", country, year)
            val response = httpClient.get(BASE_URL) {
                parameter("api_key", apiKey)
                parameter("country", country)
                parameter("year", year)
            }.body<CalendarificResponse>()

            val meta = response.meta
            if (meta == null || meta.code != 200) {
                val errorMsg =
                    "Calendarific API 응답 실패 (meta code: ${meta?.code}, errorType: ${meta?.errorType}, errorDetail: ${meta?.errorDetail})"
                logger.error(errorMsg)
                throw IllegalStateException(errorMsg)
            }

            val holidays = response.response?.holidays ?: emptyList()
            logger.info("Calendarific 휴일 조회 완료 - country: {}, year: {}, 총 {}건", country, year, holidays.size)
            holidays
        } catch (e: Exception) {
            logger.error("Calendarific 휴일 조회 중 오류 발생 - country: {}, year: {}", country, year, e)
            throw e
        }
    }

    /**
     * 현재 연도의 휴일을 조회한 후 primary_type이 "National holiday", "Bank holiday"인 항목만 필터링하여 DB에 저장합니다.
     */
    suspend fun syncHolidays(
        country: String = DEFAULT_COUNTRY
    ) {
        val currentYear = getCurrentYear()
        val allHolidays = getHolidays(country, currentYear)

        // primary_type이 "national holiday" 또는 "bank holiday"인 항목만 필터링 (대소문자/공백 무시)
        val targetHolidays = allHolidays.filter { holiday ->
            holiday.primaryType?.trim()?.lowercase() in TARGET_PRIMARY_TYPES && !holiday.date?.iso.isNullOrBlank()
        }

        logger.info(
            "휴일 필터링 완료 (연도: {}, 대상: National holiday, Bank holiday) - 전체 {}건 중 {}건 대상",
            currentYear,
            allHolidays.size,
            targetHolidays.size
        )

        if (targetHolidays.isEmpty()) {
            logger.warn("저장할 휴일 데이터가 없습니다 - country: {}, year: {}", country, currentYear)
            return
        }

        withContext(Dispatchers.IO) {
            val now = Clock.System.now().toLocalDateTime(TimeZone.of("Asia/Tokyo"))
            transaction {
                Holiday.batchUpsert(targetHolidays) { holiday ->
                    val countryCode = (holiday.country?.id?.uppercase() ?: country).uppercase()
                    this[Holiday.country] = countryCode
                    this[Holiday.holidayDate] = holiday.date?.iso ?: ""
                    this[Holiday.year] = currentYear
                    this[Holiday.holidayName] = holiday.name
                    this[Holiday.holidayType] = holiday.primaryType ?: "Holiday"
                    this[Holiday.description] = holiday.description
                    this[Holiday.regDate] = now
                    this[Holiday.modDate] = now
                }
            }
        }

        logger.info("휴일 데이터 DB 저장(Upsert) 완료 - 총 {}건", targetHolidays.size)
    }

    /**
     * market 파라미터에 해당하는 country 코드로 오늘 날짜가 holiday 테이블에 존재하는지 확인합니다.
     * TSE → JP, 그 외(NYS/NAS/AMS 등) → US
     *
     * @param market 시장 코드 (e.g. "TSE", "NYS", "NAS", "AMS")
     * @return 오늘이 해당 country의 휴일이면 true, 아니면 false
     */
    fun isTodayHoliday(market: String): Boolean {
        val country = if (market == "TSE") "JP" else "US"
        val today = LocalDate.now(ZoneId.of("Asia/Tokyo")).toString() // "yyyy-MM-dd"

        val count = transaction {
            Holiday.select(Holiday.holidayDate)
                .where { (Holiday.country eq country) and (Holiday.holidayDate eq today) }
                .count()
        }

        return count > 0
    }
}
