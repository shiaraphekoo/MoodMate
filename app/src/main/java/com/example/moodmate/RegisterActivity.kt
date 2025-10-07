package com.example.moodmate

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.moodmate.databinding.ActivityRegisterBinding

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.loadAndApplyTheme(this)
        super.onCreate(savedInstanceState)

        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase instances
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // --- Handle Register Button Click ---
        binding.buttonRegister.setOnClickListener {
            val username = binding.inputUsername.text.toString().trim()
            val email = binding.inputEmail.text.toString().trim()
            val password = binding.inputPassword.text.toString()
            val confirmPassword = binding.inputConfirmPassword.text.toString()

            if (email.isEmpty() || password.isEmpty() || username.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(this, "Passwords do not match.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Create user with Firebase Auth
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val firebaseUser = auth.currentUser
                        val uid = firebaseUser?.uid

                        // 2. Save user details to Firestore 'users' collection
                        if (uid != null) {
                            saveUserToFirestore(uid, email, username)
                        }

                        Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show()
                        navigateToMain()

                    } else {
                        // Registration failed
                        Toast.makeText(this, "Registration failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }

        // --- Handle Login Link Click ---
        binding.textLinkLogin.setOnClickListener {
            finish() // Simply close this activity to go back to Login
        }
    }

    /**
     * Saves the basic user profile to the 'users' collection in Firestore.
     */
    private fun saveUserToFirestore(uid: String, email: String, username: String) {
        val userMap = hashMapOf(
            "email" to email,
            "name" to username,
            "createdAt" to com.google.firebase.Timestamp.now()
            // languagePreference will be added by Settings if it's not the default
        )

        // Use the UID as the document ID for easy lookup
        db.collection("users").document(uid)
            .set(userMap)
            .addOnSuccessListener {
                // Log success
                println("User data saved successfully to Firestore.")
            }
            .addOnFailureListener { e ->
                // Log failure
                System.err.println("Error adding user data to Firestore: $e")
            }
    }

    /**
     * Navigates to the main mood logging activity.
     */
    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        // Clear back stack so user can't press back to login screen
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }
}
