package com.example.moodmate

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.moodmate.databinding.ActivityLogMoodBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import android.content.Context


class LogMoodActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLogMoodBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestoreDb: FirebaseFirestore

    private var selectedMood: String? = null


    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageManager.updateLanguage(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLogMoodBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        firestoreDb = FirebaseFirestore.getInstance()


        // --- Handle Mood Selection ---
        binding.moodAngry.setOnClickListener { selectMood("Angry") }
        binding.moodSad.setOnClickListener { selectMood("Sad") }
        binding.moodNeutral.setOnClickListener { selectMood("Neutral") }
        binding.moodHappy.setOnClickListener { selectMood("Happy") }
        binding.moodExcited.setOnClickListener { selectMood("Excited") }

        // --- Handle Add Photo Button Click ---
        binding.buttonAddPhoto.setOnClickListener {
            Toast.makeText(this, "Please paste your public photo URL into the dedicated field.", Toast.LENGTH_LONG).show()
        }

        // --- Handle Submit Button Click ---
        binding.buttonSubmit.setOnClickListener {
            logMood()
        }
    }

    private fun selectMood(mood: String) {
        selectedMood = mood
        Toast.makeText(this, "Selected: $selectedMood", Toast.LENGTH_SHORT).show()
    }

    /**
     * Saves the mood entry and the manually entered photo URL to Firestore.
     */
    private fun logMood() {
        if (selectedMood == null) {
            Toast.makeText(this, "Please select a mood.", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "User not authenticated. Please log in.", Toast.LENGTH_SHORT).show()
            return
        }

        val note = binding.editTextNote.text.toString()

              val photoUrlString = try {
            binding.editTextPhotoUrl.text.toString().trim()
        } catch (e: Exception) {
            Log.w("LogMoodActivity", "editTextPhotoUrl not found in layout. Using empty URL.")
            ""
        }

        // Simple validation check for a URL
        val finalPhotoUrl = if (photoUrlString.startsWith("http://") || photoUrlString.startsWith("https://")) {
            photoUrlString
        } else if (photoUrlString.isNotEmpty()) {
            Toast.makeText(this, "Photo URL must start with http:// or https://", Toast.LENGTH_LONG).show()
            ""
        } else {
            ""
        }


        // Create a new mood entry object as a HashMap
        val moodEntry = hashMapOf(
            "userId" to userId,
            "mood" to selectedMood,
            "note" to note,
            "timestamp" to Timestamp.now(),
            "photoURL" to finalPhotoUrl // Saves the manually entered URL (or empty string)
        )

        // Save the data to the 'moodEntries' collection in Firestore
        firestoreDb.collection("moodEntries")
            .add(moodEntry)
            .addOnSuccessListener {
                Toast.makeText(this, "Mood logged successfully!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Log.e("LogMoodActivity", "Firestore write failed: ${e.message}", e)
                Toast.makeText(this, "Error logging mood: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}