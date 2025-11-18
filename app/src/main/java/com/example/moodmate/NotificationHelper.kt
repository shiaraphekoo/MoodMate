package com.example.moodmate

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
import android.Manifest // Import the Manifest class

object NotificationHelper {

    private const val CHANNEL_ID = "moodmate_reminder_channel"
    private const val CHANNEL_NAME = "Daily Mood Reminder"
    private const val NOTIFICATION_ID = 101

    /**
     * Creates the Notification Channel required for Android 8.0 (Oreo) and above.
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = "Channel for daily reminders to log your mood."
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Builds and displays the daily reminder notification.
     */
    fun showReminderNotification(context: Context, title: String, message: String) {
        // Create an explicit intent for an Activity to open upon notification click
        val intent = Intent(context, LogMoodActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Build the notification
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)

            .setSmallIcon(android.R.drawable.ic_dialog_dialer)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent) // Set the intent to be triggered when the user clicks
            .setAutoCancel(true) // Dismiss the notification when the user taps it


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                // If permission is not granted (user denied it or hasn't been asked yet), skip the notification.
                // In a real app, you would ask for this permission from an Activity.
                return
            }
        }
        // --- END Permission Check ---

        with(NotificationManagerCompat.from(context)) {
            notify(NOTIFICATION_ID, builder.build())
        }
    }
}