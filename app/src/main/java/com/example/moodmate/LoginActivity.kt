package com.example.moodmate

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.example.moodmate.databinding.ActivityLoginBinding
import java.util.concurrent.Executor
import android.content.Context

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth

    // Variables for Biometric Authentication
    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageManager.updateLanguage(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // 1. Load the theme before calling super.onCreate()
        ThemeManager.loadAndApplyTheme(this)
        super.onCreate(savedInstanceState)

        // Setup View Binding
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Ensure biometric variables are initialized for later use
        executor = ContextCompat.getMainExecutor(this)

        // --- Handle Login Button Click (Standard Email/Password) ---
        binding.buttonLogin.setOnClickListener {
            performLogin()
        }

        // --- Handle Biometric Button Click (Explicit Trigger) ---
        binding.buttonBiometricLogin.setOnClickListener {
            if (auth.currentUser != null) {
                setupBiometricLogin()
            } else {
                Toast.makeText(this, "Please log in once with credentials first.", Toast.LENGTH_LONG).show()
            }
        }

        // --- Handle Register Link Click ---
        binding.textLinkRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    /**
     * Retrieves email and password, then attempts to sign in using Firebase Auth.
     */
    private fun performLogin() {
        val email = binding.editTextEmail.text.toString().trim()
        val password = binding.editTextPassword.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter both email and password.", Toast.LENGTH_SHORT).show()
            return
        }

        // Firebase Sign In
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, navigate to the main activity
                    Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()
                    navigateToMain()
                } else {
                    // If sign in fails, display a message to the user.
                    Toast.makeText(this, "Authentication failed: ${task.exception?.localizedMessage}",
                        Toast.LENGTH_LONG).show()
                }
            }
    }


    /**
     * Initializes and runs the biometric login flow.
     */
    private fun setupBiometricLogin() {

        // 1. Create a BiometricPrompt callback
        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    // If the user cancels, or an error occurs.
                    Toast.makeText(applicationContext, "Biometric Auth Error: $errString", Toast.LENGTH_SHORT).show()
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    Toast.makeText(applicationContext, "Biometric Login Successful!", Toast.LENGTH_SHORT).show()
                    navigateToMain()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    // Biometric sensor read the input, but it didn't match.
                    Toast.makeText(applicationContext, "Biometric authentication failed.", Toast.LENGTH_SHORT).show()
                }
            })

        // 2. Create the PromptInfo
        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("MoodMate Quick Login")
            .setSubtitle("Use your fingerprint or face to log in.")
            // Allow the user to use device credentials (PIN, pattern, password) if biometrics fail.
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build()

        // 3. Check for Biometric availability and display the prompt
        checkBiometricSupportAndPrompt()
    }

    /**
     * Checks if the device supports biometrics and then displays the prompt.
     */
    private fun checkBiometricSupportAndPrompt() {
        val biometricManager = BiometricManager.from(this)
        when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)) {

            BiometricManager.BIOMETRIC_SUCCESS -> {
                // Biometrics is available and enrolled. Show the prompt.
                biometricPrompt.authenticate(promptInfo)
            }

            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                // Device does not have biometric capabilities
                Toast.makeText(this, "No biometric hardware detected.", Toast.LENGTH_LONG).show()
            }

            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                // Biometric hardware is currently unavailable
                Toast.makeText(this, "Biometric hardware unavailable.", Toast.LENGTH_LONG).show()
            }

            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                // Biometrics is supported but no credentials are set up.
                Toast.makeText(this, "No biometrics enrolled. Using device PIN/Password.", Toast.LENGTH_SHORT).show()
                // The prompt will still appear, offering the fallback option.
                biometricPrompt.authenticate(promptInfo)
            }
            else -> {
                // Other errors, like BIOMETRIC_ERROR_SECURITY_DISABLED
                Toast.makeText(this, "Biometric check failed.", Toast.LENGTH_LONG).show()
            }
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