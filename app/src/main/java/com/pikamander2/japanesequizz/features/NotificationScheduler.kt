package com.pikamander2.japanesequizz.features

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.preference.PreferenceManager
import com.pikamander2.japanesequizz.MainActivity
import com.pikamander2.japanesequizz.R
import java.util.Calendar

object NotificationScheduler {
    const val CHANNEL_ID = "study_reminders"
    private const val CHANNEL_NAME = "Study Reminders"
    private val DEFAULT_HOURS = intArrayOf(10, 14, 18, 22)
    private const val BASE_REQUEST_CODE = 1000

    @JvmStatic
    fun initChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Daily study and JLPT practice reminders"
                enableVibration(true)
                enableLights(true)
            }
            nm.createNotificationChannel(channel)
        }
    }

    @JvmStatic
    fun scheduleReminders(context: Context) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val isEnabled = prefs.getBoolean("study_reminders_enabled", true)
        if (!isEnabled) {
            cancelReminders(context)
            return
        }

        initChannel(context)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val mode = prefs.getString("reminder_frequency", "multiple") ?: "multiple"

        if ("daily" == mode) {
            val timeStr = prefs.getString("notification_time", "20:00") ?: "20:00"
            val parts = timeStr.trim().split(":")
            val hour = parts.getOrNull(0)?.toIntOrNull() ?: 20
            val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
            scheduleSlot(context, alarmManager, BASE_REQUEST_CODE, hour, minute)
        } else {
            for (i in DEFAULT_HOURS.indices) {
                scheduleSlot(context, alarmManager, BASE_REQUEST_CODE + i, DEFAULT_HOURS[i], 0)
            }
        }
    }

    private fun scheduleSlot(
        context: Context,
        alarmManager: AlarmManager,
        requestCode: Int,
        hour: Int,
        minute: Int
    ) {
        val intent = Intent(context, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        } else {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }

    @JvmStatic
    fun cancelReminders(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        for (i in 0..10) {
            val intent = Intent(context, NotificationReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                BASE_REQUEST_CODE + i,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }
    }

    @JvmStatic
    fun sendNotification(context: Context) {
        initChannel(context)

        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val rawName = prefs.getString("user_name", "Bhai") ?: "Bhai"
        val name = rawName.trim().split(Regex("\\s+")).firstOrNull()?.takeIf { it.isNotBlank() } ?: "Bhai"
        val level = prefs.getString("jlpt_level", "N5") ?: "N5"

        val aggressiveAlerts = listOf(
            "Padh le $name nalayak! Baap ka paisa barbaad mat kar, $level kaun padhega?",
            "Abe aalsi ke dher $name! Phone scroll band kar aur chup chap padh le!",
            "Kitna timepass karega $name? Fail hone ka pura plan bana liya hai kya?",
            "Sharam kar thodi $name, JLPT me ande leke aayega kya? Flashcard khol abhi!",
            "Padh le $name, varna baad me roye ga exam hall me!",
            "Abe ghonchu $name, 4 ghante se ek word nahi padha tune! Jaldi revision kar!",
            "Agar abhi nahi padha $name toh $level bhul ja! Fatafat practice start kar!",
            "Bhai $name, tere se bada aalsi insaan nahi dekha maine! Khol app jaldi!",
            "Exam sar par hai $name, reel dekhna band kar aur Japanese padh!"
        )
        val alertMsg = aggressiveAlerts.random()

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pi = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notifications_black_24dp)
            .setContentTitle("Padhai Kar Le!")
            .setContentText(alertMsg)
            .setStyle(NotificationCompat.BigTextStyle().bigText(alertMsg))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(pi)
            .setAutoCancel(true)

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        nm.notify((System.currentTimeMillis() % 100000).toInt(), builder.build())
    }
}
