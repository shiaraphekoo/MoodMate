package com.example.moodmate

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.example.moodmate.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth

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

        // ---Log Mood
        binding.buttonLogMood.setOnClickListener {
            // Logic to navigate to the Log Mood screen
            val intent = Intent(this, LogMoodActivity::class.java)
            startActivity(intent)
        }

        binding.buttonCalendarView.setOnClickListener {
            // Logic to navigate to the Calendar View screen
            val intent = Intent(this, CalendarActivity::class.java)
            startActivity(intent)
        }

        binding.buttonMoodTrends.setOnClickListener {
            // Logic to navigate to the Mood Trends screen
            val intent = Intent(this, MoodTrendsActivity::class.java)
            startActivity(intent)
        }

        // --- Handle Settings Button Click ---
        binding.buttonSettings.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
 }


    override fun onResume() {
        super.onResume()

        ThemeManager.loadAndApplyTheme(this)
    }
}


//AI (Google Gemini) was used in this assignment as a tool to help me with sections I was having errors with. (Indicated in brackets)
