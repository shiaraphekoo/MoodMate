Overview

MoodMate is a modern Android mobile application built natively with Kotlin. Its primary goal is to empower users to track, visualize, and understand their emotional patterns over time. The app is designed for reliability, featuring secure Firebase Authentication, real-time data persistence via Firestore, and flexible support for localization and device themes. The focus is on providing a seamless, robust, and accessible platform for personal well-being analysis. Demo: https://youtu.be/Kul_x5PjQQs

✨ Key Features

User Experience & Functionality

Secure Authentication & Biometrics:

Supports standard Email/Password registration and login.

Includes a Biometric Quick-Login option (Fingerprint/Face ID) via the AndroidX Biometric library for users who have a valid, active Firebase session, providing fast and convenient access without needing to re-enter credentials.

Multilingual Support (Localization):

Fully localized for English (en) and Zulu (zu).

Language is changed dynamically via the Settings screen. The change is instantly applied across the entire app by leveraging the LanguageManager and the attachBaseContext lifecycle hook in every Activity.

Flexible Mood Logging:

Users can quickly select one of five defined moods: Angry, Sad, Neutral, Happy, or Excited.

A rich text field allows for adding detailed contextual notes for each entry.

Optional Photo Linking (Manual URL Input):

To overcome regional cloud storage limitations, the app avoids direct Firebase Storage uploads.

Instead, users can paste a public URL (e.g., from Imgur or a CDN) into a dedicated input field. This URL is validated and saved as a string in Firestore, enabling picture association without complex upload logic.

Theming: Supports both Light and Dark modes, configurable in the Settings screen, enhancing accessibility and user preference compliance.

Technical Architecture

Data Persistence (Firestore): All user-generated content is stored in a structured collection called moodEntries.

Data Model Structure: Each mood entry document includes essential fields:

userId: String (Authenticated user's UID)

mood: String (e.g., "Happy", "Angry")

note: String (User-entered text)

timestamp: Timestamp (Firebase server-side time for consistent logging)

photoURL: String (The manual public URL, or empty string if no photo is linked).

Firebase Dependencies: Uses the Firebase Bill of Materials (BOM) to ensure strict version alignment across all Firebase libraries (Auth, Firestore, Messaging).

AndroidX Biometric: Implements modern biometric authentication checks, including hardware availability and enrollment status, with a secure fallback to device credentials.

💻 Technical Stack

Platform: Android (API 24+)

Language: Kotlin (with Java 11 JVM target)

UI Framework: ConstraintLayout with View Binding for efficient layout management.

Core Libraries:

com.google.firebase:firebase-auth-ktx (Authentication)

com.google.firebase:firebase-firestore-ktx (Database)

androidx.biometric:biometric (Quick login)

com.google.firebase:firebase-bom (Dependency Management)

🚀 Setup, Installation, and Running the App

Follow these steps to clone the repository and configure the project in Android Studio.

Prerequisites

Android Studio: Ensure you have the latest version of Android Studio installed.

JDK 11 or higher: Required for Kotlin compilation (usually bundled with Android Studio).

Firebase Account: A Google account with access to the Firebase Console is necessary.

Installation Steps

Clone the Repository:

git clone [YOUR_REPO_URL]
cd moodmate


Open in Android Studio:

Open Android Studio and select File > Open. Navigate to the moodmate directory and open the project.

Allow Gradle to sync dependencies (check the Gradle console for any issues).

Firebase Configuration (Crucial):

a.  Create Firebase Project: Go to the [Firebase Console] and create a new project.
b.  Add Android App: Register an Android app within your new Firebase project. Use the package name: com.example.moodmate.
c.  Download google-services.json: Download the configuration file provided by Firebase.
d.  Place Config File: Move the google-services.json file into the app/ directory of your local project structure.

Firebase Service Setup

You must enable and configure the following services in your Firebase Console:

Enable Authentication:

Navigate to Authentication > Sign-in method.

Enable the Email/Password provider.

Enable Firestore Database:

Navigate to Firestore Database > Create database.

Start in production mode and choose your security location.

Set Security Rules (Mandatory):
The app requires all authenticated users to read and write data. Update your Firestore Rules as follows:

rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /{document=**} {
      // Allows read/write access only if the user is signed in
      allow read, write: if request.auth != null; 
    }
  }
}


Running the Application

Connect an Android device or launch an Android Emulator.

Click the Run button (green arrow) in Android Studio.

The app should launch on the device/emulator, starting with the Login/Registration screen.

🌐 Localization Resources

All multilingual strings are defined in the following resource files:

res/values/strings.xml (Default/English)

res/values-zu/strings.xml (Zulu)
The LanguageManager and attachBaseContext override in every Activity ensure that the correct strings are loaded at runtime, maintaining a consistent experience when switching languages.
