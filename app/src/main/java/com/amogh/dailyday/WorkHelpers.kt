package com.amogh.dailyday

import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

object WorkHelpers {
    const val UNIQUE_WORK_NAME = "daily_image_work"

    fun minutesUntilNextRun(hour: Int, minute: Int): Long {
        val now = ZonedDateTime.now()
        var next = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        if (!next.isAfter(now)) next = next.plusDays(1)
        return ChronoUnit.MINUTES.between(now, next)
    }
}
