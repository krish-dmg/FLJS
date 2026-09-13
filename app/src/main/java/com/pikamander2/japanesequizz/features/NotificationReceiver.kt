package com.pikamander2.japanesequizz.features

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        NotificationScheduler.sendNotification(context)
        NotificationScheduler.scheduleReminders(context)
    }
}
