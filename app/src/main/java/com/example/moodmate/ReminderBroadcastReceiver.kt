package com.example.moodmate

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class ReminderBroadcastReceiver : BroadcastReceiver() {

    // Define the reminder time
    private val REMINDER_TITLE = "Time to Check In!"
    private val REMINDER_MESSAGE = "How was your day? Take a moment to log your current mood and thoughts."

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("Reminder", "Alarm triggered. Displaying mood reminder notification.")

        // Ensure the notification channel exists
        NotificationHelper.createNotificationChannel(context)

        // Show the actual notification
        NotificationHelper.showReminderNotification(
            context,
            REMINDER_TITLE,
            REMINDER_MESSAGE
        )


    }
}