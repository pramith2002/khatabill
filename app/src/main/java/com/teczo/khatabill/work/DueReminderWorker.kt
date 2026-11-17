package com.teczo.khatabill.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.teczo.khatabill.data.dao.BusinessDao
import com.teczo.khatabill.data.dao.InvoiceDao
import com.teczo.khatabill.data.dao.PartyDao
import com.teczo.khatabill.data.dao.ReminderDao
import com.teczo.khatabill.data.settings.SettingsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

/** Runs once a day in the morning (see [ReminderScheduler]). Does nothing when reminders are switched off. */
@HiltWorker
class DueReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val settings: SettingsRepository,
    private val businessDao: BusinessDao,
    private val invoiceDao: InvoiceDao,
    private val partyDao: PartyDao,
    private val reminderDao: ReminderDao,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (!settings.remindersEnabled.first()) return Result.success()
        return try {
            DueReminderRunner(applicationContext, businessDao, invoiceDao, partyDao, reminderDao).run()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
