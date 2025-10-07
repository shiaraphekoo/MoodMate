package com.example.moodmate

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.moodmate.databinding.ActivityLogMoodBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Date

class LogMoodActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLogMoodBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestoreDb: FirebaseFirestore
    private var selectedMood: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLogMoodBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        firestoreDb = FirebaseFirestore.getInstance()

        // --- Handle Mood Selection ---
        // These IDs must match the IDs of your mood emoji ImageViews in the XML
        binding.moodAngry.setOnClickListener { selectMood("Angry") }
        binding.moodSad.setOnClickListener { selectMood("Sad") }
        binding.moodNeutral.setOnClickListener { selectMood("Neutral") }
        binding.moodHappy.setOnClickListener { selectMood("Happy") }
        binding.moodExcited.setOnClickListener { selectMood("Excited") }

        // --- Handle Submit Button Click ---
        binding.buttonSubmit.setOnClickListener {
            logMood()
        }
    }

    private fun selectMood(mood: String) {
        selectedMood = mood
        // Here you can add logic to visually highlight the selected mood emoji
        Toast.makeText(this, "Selected: $selectedMood", Toast.LENGTH_SHORT).show()
    }

    private fun logMood() {
        if (selectedMood == null) {
            Toast.makeText(this, "Please select a mood.", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "User not authenticated.", Toast.LENGTH_SHORT).show()
            return
        }

        val note = binding.editTextNote.text.toString()

        // Create a new mood entry object as a HashMap
        val moodEntry = hashMapOf(
            "userId" to userId,
            "mood" to selectedMood,
            "note" to note,
            "timestamp" to Date(), // Firestore handles Date objects correctly
            "photoURL" to "" // Placeholder for photo URL functionality
        )

        // Save the data to the 'moodEntries' collection in Firestore
        firestoreDb.collection("moodEntries")
            .add(moodEntry)
            .addOnSuccessListener {
                Toast.makeText(this, "Mood logged successfully!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error logging mood: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}