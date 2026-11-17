package com.teczo.khatabill.work

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.teczo.khatabill.MainActivity
import com.teczo.khatabill.R
import com.teczo.khatabill.data.dao.BusinessDao
import com.teczo.khatabill.data.dao.InvoiceDao
import com.teczo.khatabill.data.dao.PartyDao
import com.teczo.khatabill.data.dao.ReminderDao
import com.teczo.khatabill.data.entities.ReminderEntity
import com.teczo.khatabill.domain.model.ReminderChannel
import com.teczo.khatabill.domain.model.ReminderStatus
import com.teczo.khatabill.util.dates.DateFormat
import com.teczo.khatabill.util.locale.LocalizedContext
import com.teczo.khatabill.util.money.MoneyFormatter
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

/**
 * Finds bills that are due, raises one local notification per party ("Ramesh owes ₹12,400, due today"),
 * and logs a Reminder row so it is not repeated the same day. Works fully offline.
 */
class DueReminderRunner(
    private val context: Context,
    private val businessDao: BusinessDao,
    private val invoiceDao: InvoiceDao,
    private val partyDao: PartyDao,
    private val reminderDao: ReminderDao,
) {
    /** Returns the notices that were actually shown (none when notification permission is missing). */
    suspend fun run(today: Long = DateFormat.today()): List<DueNotice> {
        val business = businessDao.observeFirst().first() ?: return emptyList()
        val due = invoiceDao.getDueUpTo(business.id, today)
        if (due.isEmpty()) return emptyList()

        val parties = due.mapNotNull { it.partyId }.distinct().mapNotNull { partyDao.getById(it) }.associateBy { it.id }
        val startOfDay = LocalDate.ofEpochDay(today).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val already = due.filter { reminderDao.countSince(it.id, ReminderChannel.NOTIFICATION, startOfDay) > 0 }.map { it.id }.toSet()
        val notices = DueReminders.plan(due, parties, already, today)
        if (notices.isEmpty() || !canNotify()) return emptyList()

        ensureChannel()
        val localized = LocalizedContext.forApp(context)
        val kannada = LocalizedContext.appLanguageTag() == "kn"
        val now = System.currentTimeMillis()
        notices.forEachIndexed { index, notice ->
            val name = if (kannada) notice.partyNameKn?.takeIf { it.isNotBlank() } ?: notice.partyName else notice.partyName
            val text = localized.getString(
                if (notice.overdue) R.string.notif_overdue else R.string.notif_due_today,
                name, "₹" + MoneyFormatter.format(notice.amount),
            )
            val open = PendingIntent.getActivity(
                context, index, Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            val n = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle(localized.getString(R.string.app_name))
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setContentIntent(open)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()
            @Suppress("MissingPermission") // checked in canNotify()
            NotificationManagerCompat.from(context).notify(notice.partyId.hashCode(), n)
            notice.invoiceIds.forEach { invoiceId ->
                reminderDao.insert(
                    ReminderEntity(
                        id = UUID.randomUUID().toString(), businessId = business.id, createdAt = now, updatedAt = now,
                        partyId = notice.partyId, invoiceId = invoiceId, scheduledAt = now,
                        channel = ReminderChannel.NOTIFICATION, status = ReminderStatus.SENT, message = text,
                    ),
                )
            }
        }
        return notices
    }

    private fun canNotify(): Boolean {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return false
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val manager = context.getSystemService(NotificationManager::class.java)
            val name = LocalizedContext.forApp(context).getString(R.string.notif_channel_name)
            manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_DEFAULT))
        }
    }

    companion object {
        const val CHANNEL_ID = "due-reminders"
    }
}
