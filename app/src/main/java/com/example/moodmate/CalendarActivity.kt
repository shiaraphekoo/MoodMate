package com.example.moodmate

import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.moodmate.databinding.ActivityCalendarViewBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class CalendarActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCalendarViewBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestoreDb: FirebaseFirestore

    private val TAG = "CalendarActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Corrected binding class name
        binding = ActivityCalendarViewBinding.inflate(layoutInflater)
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

        // Fetch moods for the current user
        firestoreDb.collection("moodEntries")
            .whereEqualTo("userId", userId)
            .orderBy("timestamp")
            .get()
            .addOnSuccessListener { result ->
                // Remove the initial content/loading text
                binding.calendarContainer.removeAllViews()

                if (result.isEmpty) {
                    val noDataText = TextView(this).apply {
                        text = "No mood entries found yet. Log a mood!"
                        textSize = 20f
                        gravity = Gravity.CENTER
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.MATCH_PARENT
                        )
                    }
                    binding.calendarContainer.addView(noDataText)
                    return@addOnSuccessListener
                }

                val moodEntries = mutableMapOf<Int, String>() // Day of Month -> Mood
                val dayOfMonthFormat = SimpleDateFormat("dd", Locale.getDefault())

                result.documents.forEach { doc ->
                    val timestamp = doc.getDate("timestamp")
                    val mood = doc.getString("mood")

                    if (timestamp != null && mood != null) {
                        // Extract the day of the month from the timestamp
                        val dayOfMonth = dayOfMonthFormat.format(timestamp).toInt()
                        // Store the latest mood for that specific day
                        moodEntries[dayOfMonth] = mood
                    }
                }

                // Update the UI to display the fetched moods on the calendar grid
                updateCalendarView(moodEntries)

            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error getting mood data: ", exception)
                Toast.makeText(this, "Failed to load calendar data. Check index.", Toast.LENGTH_LONG).show()
                binding.calendarContainer.removeAllViews()
                val errorText = TextView(this).apply {
                    text = "Failed to load moods. Please check connection."
                    gravity = Gravity.CENTER
                }
                binding.calendarContainer.addView(errorText)
            }
    }

    /**
     * Creates and populates the custom calendar grid inside calendar_container.
     * @param moodData Map of Day of Month (1-31) to Mood String ("Angry", "Happy", etc.)
     */
    private fun updateCalendarView(moodData: Map<Int, String>) {
        val calendar = Calendar.getInstance()
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
        val maxDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val firstDayOfMonth = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1) }
        // Determine the offset (number of blank cells before the 1st). 0 for Sunday.
        val offset = firstDayOfMonth.get(Calendar.DAY_OF_WEEK) - 1

        // Calculate a fixed height for each row to prevent collapsing
        // We assume 7 rows max (1 header + 6 weeks)
        val minCalendarHeightDp = 300 // Based on the minHeight in your XML
        val calendarGridHeightPx = dpToPx(minCalendarHeightDp)
        val cellHeightPx = calendarGridHeightPx / 7 // Divide by 7 rows (1 header + 6 weeks)

        // 1. Create the GridLayout
        val calendarGrid = GridLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT // MATCH_PARENT uses the available container height
            )
            columnCount = 7 // Sunday to Saturday
        }

        // 2. Add Day Headers (S, M, T, W, T, F, S)
        val daysOfWeek = arrayOf("S", "M", "T", "W", "T", "F", "S")
        daysOfWeek.forEach { day ->
            val dayView = TextView(this).apply {
                text = day
                textSize = 14f
                gravity = Gravity.CENTER
                setPadding(0, dpToPx(4), 0, dpToPx(4))
                layoutParams = GridLayout.LayoutParams(
                    GridLayout.spec(GridLayout.UNDEFINED, 1f),
                    GridLayout.spec(GridLayout.UNDEFINED, 1f)
                ).apply {
                    width = 0
                    height = cellHeightPx // Force height for header row
                }
            }
            calendarGrid.addView(dayView)
        }

        // 3. Add Offset/Blank Cells
        for (i in 0 until offset) {
            val blankView = View(this)
            calendarGrid.addView(blankView, getGridLayoutParams(cellHeightPx))
        }

        // 4. Add Day Cells with Mood Dots
        for (day in 1..maxDays) {
            val dayCell = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4))
                layoutParams = getGridLayoutParams(cellHeightPx) // Use calculated height
                // Optionally highlight the current day
                if (day == currentDay) {

                    setBackgroundResource(R.drawable.rounded_day_highlight)
                }
            }

            // Day Number
            val dayNumText = TextView(this).apply {
                text = day.toString()
                textSize = 12f
                gravity = Gravity.CENTER
            }
            dayCell.addView(dayNumText)

            // Mood Dot
            val mood = moodData[day]
            val moodDot = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(dpToPx(8), dpToPx(8)).apply {
                    setMargins(0, dpToPx(4), 0, 0)
                    gravity = Gravity.CENTER_HORIZONTAL
                }
                background = createMoodDotDrawable(mood)
            }
            dayCell.addView(moodDot)
            calendarGrid.addView(dayCell)
        }

        // 5. Add the generated grid to the container
        binding.calendarContainer.addView(calendarGrid)
    }

    /** Helper to create the colored mood dot drawable */
    private fun createMoodDotDrawable(mood: String?): GradientDrawable {
        val color = when (mood) {
            "Angry" -> Color.parseColor("#E53935") // Red
            "Sad" -> Color.parseColor("#42A5F5")   // Blue
            "Neutral" -> Color.parseColor("#FFCA28") // Yellow
            "Happy" -> Color.parseColor("#66BB6A") // Green
            "Excited" -> Color.parseColor("#7E57C2") // Purple
            else -> Color.TRANSPARENT // No mood logged
        }

        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            if (mood != null) {
                setColor(color)
            }
            setSize(dpToPx(8), dpToPx(8))
        }
    }

    /** Helper for standard GridLayout.LayoutParams, including height (Google Gemini, 2025)*/
    private fun getGridLayoutParams(heightPx: Int): GridLayout.LayoutParams {
        return GridLayout.LayoutParams(
            GridLayout.spec(GridLayout.UNDEFINED, 1f),
            GridLayout.spec(GridLayout.UNDEFINED, 1f)
        ).apply {
            width = 0
            height = heightPx // Explicitly set the calculated cell height
        }
    }

    /** Helper to convert DP to Pixels using application context resources (Google Gemini, 2025) */
    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            applicationContext.resources.displayMetrics
        ).toInt()
    }
}
