package com.example.moodmate

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.example.moodmate.databinding.ActivitySettingsBinding
import com.example.moodmate.ThemeManager.Theme // Import the enum

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.loadAndApplyTheme(this)
        super.onCreate(savedInstanceState)

        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        setupThemeSelector()
        setupLogoutButton()
    }

    /**
     * Initializes the radio buttons based on the current theme and sets up listeners.
     */
    private fun setupThemeSelector() {
        // Fix 1: Use getCurrentTheme() and compare with the Theme enum
        val currentTheme = ThemeManager.getCurrentTheme(this)
        if (currentTheme == Theme.DARK) {
            binding.radioDark.isChecked = true
        } else {
            binding.radioLight.isChecked = true
        }

        binding.radioGroupTheme.setOnCheckedChangeListener { _, checkedId ->
            // Fix 2: Pass the Theme enum to saveTheme()
            val themeToSave = when (checkedId) {
                binding.radioLight.id -> Theme.LIGHT
                binding.radioDark.id -> Theme.DARK
                else -> Theme.LIGHT
            }

            ThemeManager.saveTheme(this, themeToSave)

            // Recreate the activity to apply the theme change immediately
            recreate()
        }
    }

    /**
     * Sets up the logout functionality.
     */
    private fun setupLogoutButton() {
        binding.buttonLogout.setOnClickListener {
            auth.signOut()
            Toast.makeText(this, "Logged out successfully.", Toast.LENGTH_SHORT).show()

            // Navigate back to LoginActivity and clear the activity stack
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }
}