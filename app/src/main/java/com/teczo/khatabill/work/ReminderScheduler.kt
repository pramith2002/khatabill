package com.teczo.khatabill.work

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

object ReminderScheduler {
    private const val NAME = "due-reminders"

    /** Delay from [now] to the next 9:00 in the morning (today if it has not passed yet). */
    fun delayToNextNineAm(now: LocalDateTime = LocalDateTime.now()): Duration {
        var next = now.toLocalDate().atTime(LocalTime.of(9, 0))
        if (!next.isAfter(now)) next = next.plusDays(1)
        return Duration.between(now, next)
    }

    /** Schedules the daily due-date check. Safe to call on every app start: an existing schedule is kept. */
    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<DueReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(delayToNextNineAm().toMinutes(), TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }
}
