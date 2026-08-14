package com.ietf.etfbatch.holiday.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CalendarificResponse(
    val meta: CalendarificMeta? = null,
    val response: CalendarificHolidayData? = null
)

@Serializable
data class CalendarificMeta(
    val code: Int? = null,
    @SerialName("error_type")
    val errorType: String? = null,
    @SerialName("error_detail")
    val errorDetail: String? = null
)

@Serializable
data class CalendarificHolidayData(
    val holidays: List<HolidayDto> = emptyList()
)

@Serializable
data class HolidayDto(
    val name: String = "",
    val description: String? = null,
    val country: HolidayCountry? = null,
    val date: HolidayDate? = null,
    val type: List<String> = emptyList(),
    @SerialName("primary_type")
    val primaryType: String? = null,
    @SerialName("canonical_url")
    val canonicalUrl: String? = null,
    val urlid: String? = null,
    val locations: String? = null,
    val states: String? = null
)

@Serializable
data class HolidayCountry(
    val id: String? = null,
    val name: String? = null
)

@Serializable
data class HolidayDate(
    val iso: String? = null,
    val datetime: HolidayDateTime? = null
)

@Serializable
data class HolidayDateTime(
    val year: Int? = null,
    val month: Int? = null,
    val day: Int? = null,
    val hour: Int? = null,
    val minute: Int? = null,
    val second: Int? = null
)
