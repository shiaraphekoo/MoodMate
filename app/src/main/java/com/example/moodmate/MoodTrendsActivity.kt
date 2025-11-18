package com.example.moodmate

import android.content.Context
import android.os.Bundle
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF // Import RectF for drawing rectangles
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.moodmate.databinding.ActivityMoodTrendsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


class MoodTrendsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMoodTrendsBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestoreDb: FirebaseFirestore

    private val TAG = "MoodTrendsActivity"

    // Map to assign numerical values for graphing
    private val moodValueMap = mapOf(
        "Angry" to 1.0f,
        "Sad" to 2.0f,
        "Neutral" to 3.0f,
        "Happy" to 4.0f,
        "Excited" to 5.0f
    )
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageManager.updateLanguage(newBase))
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMoodTrendsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        firestoreDb = FirebaseFirestore.getInstance()

        // Set the current month selector text
        val currentMonthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        binding.currentMonthSelector.text = currentMonthFormat.format(Date())

        fetchMoodData()
    }

    private fun fetchMoodData() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "User not authenticated. Please log in.", Toast.LENGTH_LONG).show()
            return
        }

        // Calculate the range for the last 30 days
        val calendar = Calendar.getInstance()
        val endDate = calendar.time
        calendar.add(Calendar.DAY_OF_YEAR, -30) // Go back 30 days
        val startDate = calendar.time

        // 1. Fetch moods for the current user within the date range

        firestoreDb.collection("moodEntries")
            .whereEqualTo("userId", userId)
            .whereGreaterThanOrEqualTo("timestamp", startDate)
            .whereLessThanOrEqualTo("timestamp", endDate)
            .orderBy("timestamp")
            .get()
            .addOnSuccessListener { result ->
                if (result.isEmpty) {
                    binding.moodGraphContainer.removeAllViews()
                    binding.moodGraphContainer.addView(createNoDataView())
                    return@addOnSuccessListener
                }

                // 2. Process data and convert to numerical points
                val datePoints = processMoodData(result.documents)

                // 3. Update the UI with the graph
                updateGraphView(datePoints)

            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error getting mood data: ", exception)
                Toast.makeText(this, "Failed to load trend data. Check Firebase indexes.", Toast.LENGTH_LONG).show()
                binding.moodGraphContainer.removeAllViews()
                binding.moodGraphContainer.addView(createErrorView())
            }
    }

    /**
     * Converts raw Firestore documents into a list of 30 daily mood values.
     * Missing days are filled with the Neutral (3.0f) value. (Google Gemini, 2025)
     */
    private fun processMoodData(documents: List<DocumentSnapshot>): List<Float> {
        val calendar = Calendar.getInstance()
        val todayDayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        // Create a map to store the latest mood value for each day (key: yyyy-MM-dd)
        val dailyMoods = mutableMapOf<String, Float>()

        for (doc in documents) {
            val timestamp = doc.getDate("timestamp")
            val mood = doc.getString("mood")

            if (timestamp != null && mood != null) {
                val dateKey = dateFormat.format(timestamp)
                val moodValue = moodValueMap[mood] ?: 3.0f // Default to neutral

                // Store the latest mood value for that specific date
                dailyMoods[dateKey] = moodValue
            }
        }

        // Generate the final list of 30 data points, filling in gaps with 3.0f (Neutral)
        val finalPoints = mutableListOf<Float>()

        // Start 29 days ago
        calendar.time = Date()
        calendar.add(Calendar.DAY_OF_YEAR, -29)

        for (i in 0 until 30) {
            val dateKey = dateFormat.format(calendar.time)
            val moodValue = dailyMoods[dateKey] ?: 3.0f // Fill missing days with Neutral
            finalPoints.add(moodValue)

            // Move to the next day
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        return finalPoints
    }

    private fun updateGraphView(dataPoints: List<Float>) {
        binding.moodGraphContainer.removeAllViews()
        // Pass the calculated points and context to the custom view
        val graphView = MoodGraphView(this, dataPoints)
        binding.moodGraphContainer.addView(graphView)
    }

    private fun createNoDataView(): View {
        return TextView(this).apply {
            text = "No mood trends found for the last 30 days. Log a mood!"
            textSize = 18f
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }
    }

    private fun createErrorView(): View {
        return TextView(this).apply {
            text = "Failed to load trends. Please check your connection or Firebase indexes."
            textSize = 18f
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }
    }


    /**
     * Custom View to draw the bar graph of mood values over 30 days.
     */
    class MoodGraphView @JvmOverloads constructor(
        context: Context,
        private var dataPoints: List<Float> = emptyList(),
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
    ) : View(context, attrs, defStyleAttr) {

        private val barPaint = Paint().apply {
            color = Color.parseColor("#7E57C2") // Purple bar color
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        private val baselinePaint = Paint().apply {
            color = Color.DKGRAY
            strokeWidth = 2f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }

        // Called when the view is drawn
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)

            if (dataPoints.isEmpty()) {

                return
            }

            val padding = 50f
            val graphWidth = width - padding * 2
            val graphHeight = height - padding * 2

            // Mood values are normalized between 1.0 (Angry) and 5.0 (Excited).
            val maxValue = 5.0f
            val minValue = 1.0f
            val valueRange = maxValue - minValue // 4.0f

            // Draw baseline (representing "Angry" or the minimum mood level)
            val baselineY = padding + ((maxValue - minValue) / valueRange * graphHeight) // Y position for value 1.0 (Angry)
            canvas.drawLine(padding, baselineY, width - padding, baselineY, baselinePaint)

            // Calculate bar width and spacing
            val numberOfBars = dataPoints.size
            val barWidth = graphWidth / (numberOfBars * 1.5f) // Adjust multiplier for spacing
            val barSpacing = (graphWidth - (numberOfBars * barWidth)) / (numberOfBars - 1).coerceAtLeast(1) // Evenly space bars

            dataPoints.forEachIndexed { index, value ->
                // Calculate the center of the bar
                val xCenter = padding + (index * (barWidth + barSpacing)) + (barWidth / 2)

                // The height of the bar is proportional to the mood value. (Google Gemini, 2025)
                // A higher mood value means a taller bar, extending upwards from the baseline.
                val barHeight = ((value - minValue) / valueRange) * graphHeight

                // The bottom of the bar will be at the baselineY (representing minValue, e.g., Angry=1.0)
                // The top of the bar will be baselineY - barHeight (since Y increases downwards)
                val topOfBar = baselineY - barHeight
                val bottomOfBar = baselineY

                val left = xCenter - (barWidth / 2)
                val right = xCenter + (barWidth / 2)

                // Ensure bars don't go below graph bottom if mood is very low or equal to minValue
                val actualTopOfBar = topOfBar.coerceAtMost(bottomOfBar) // Top should not be below bottom

                val barRect = RectF(left, actualTopOfBar, right, bottomOfBar)
                canvas.drawRect(barRect, barPaint)
            }
        }
    }
}