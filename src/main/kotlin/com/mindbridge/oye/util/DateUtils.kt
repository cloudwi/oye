package com.mindbridge.oye.util

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

object DateUtils {
    val KST: ZoneId = ZoneId.of("Asia/Seoul")

    fun today(): LocalDate = LocalDate.now(KST)

    fun getDayOfWeekKorean(date: LocalDate): String {
        return when (date.dayOfWeek) {
            DayOfWeek.MONDAY -> "월요일"
            DayOfWeek.TUESDAY -> "화요일"
            DayOfWeek.WEDNESDAY -> "수요일"
            DayOfWeek.THURSDAY -> "목요일"
            DayOfWeek.FRIDAY -> "금요일"
            DayOfWeek.SATURDAY -> "토요일"
            DayOfWeek.SUNDAY -> "일요일"
        }
    }
}
