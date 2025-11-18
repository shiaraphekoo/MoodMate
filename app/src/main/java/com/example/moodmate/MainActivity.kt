package com.example.moodmate

import android.Manifest // Required for notification permission check
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat // Required for runtime permission request
import androidx.core.content.ContextCompat // Required for runtime permission check
import com.google.firebase.auth.FirebaseAuth
import com.example.moodmate.databinding.ActivityMainBinding
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth

    // Constant for the permission request code
    private val REQUEST_NOTIFICATION_PERMISSION = 1001

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageManager.updateLanguage(newBase))
    }
    override fun onCreate(savedInstanceState: Bundle?) {

        ThemeManager.loadAndApplyTheme(this)
        super.onCreate(savedInstanceState)


        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        // Safety check: If for any reason we land here without a user, go back to login.
        if (auth.currentUser == null) {
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            return
        }

        // --- NOTIFICATION PERMISSION REQUEST (API 33+) ---
        // Asks user for permission to send notifications when the main screen loads
        requestNotificationPermission()
        // --- END PERMISSION REQUEST ---

        // --- Navigation Buttons ---
        binding.buttonLogMood.setOnClickListener {
            val intent = Intent(this, LogMoodActivity::class.java)
            startActivity(intent)
        }

        binding.buttonCalendarView.setOnClickListener {
            val intent = Intent(this, CalendarActivity::class.java)
            startActivity(intent)
        }

        binding.buttonMoodTrends.setOnClickListener {
            val intent = Intent(this, MoodTrendsActivity::class.java)
            startActivity(intent)
        }

        binding.buttonSettings.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        // Schedule the reminder after the user is confirmed logged in
        scheduleDailyReminder()
    }

    // --- NOTIFICATION PERMISSION LOGIC ---
    /**
     * Checks if notification permission is needed (API 33+) and requests it from the user.
     */
    private fun requestNotificationPermission() {
        // Check if the device runs Android 13 (TIRAMISU) or higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {

                // Request the permission from the user
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_NOTIFICATION_PERMISSION
                )
            }
        }
    }

    /**
     * Handles the result of the permission request dialogue.
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_NOTIFICATION_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Notifications enabled!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Reminder notifications disabled.", Toast.LENGTH_LONG).show()
            }
        }
    }
    // --- END NOTIFICATION PERMISSION LOGIC ---

    override fun onResume() {
        super.onResume()
        ThemeManager.loadAndApplyTheme(this)
    }

    /**
     * Sets up a daily repeating inexact alarm to remind the user to log their mood
     * at 8:00 PM every day.
     */
    private fun scheduleDailyReminder() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // 1. Define the time for the reminder (e.g., 8:00 PM)
        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, 20) // 20:00 (8 PM)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)

            // If the reminder time has already passed today, set it for tomorrow.
            if (timeInMillis < System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        // 2. Create the PendingIntent to launch the BroadcastReceiver
        val intent = Intent(this, ReminderBroadcastReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Stop any existing alarm to prevent duplicates from stacking up
        alarmManager.cancel(pendingIntent)

        // 3. Set the repeating alarm.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // For modern Android devices: uses an inexact alarm which is battery-friendly.
            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                AlarmManager.INTERVAL_DAY, // Repeats every 24 hours
                pendingIntent
            )
        } else {
            // For older APIs
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                pendingIntent
            )
        }

        Toast.makeText(this, "Daily mood reminder set for 8:00 PM.", Toast.LENGTH_SHORT).show()
    }
}