package com.example.moodmate

import android.content.Context
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
import com.google.firebase.firestore.Query
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

import com.example.moodmate.ThemeManager

class CalendarActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCalendarViewBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestoreDb: FirebaseFirestore

    private val TAG = "CalendarActivity"

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageManager.updateLanguage(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.loadAndApplyTheme(this)

        super.onCreate(savedInstanceState)
        binding = ActivityCalendarViewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        firestoreDb = FirebaseFirestore.getInstance()

        // Set the current month selector text
        val currentMonthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        val currentCalendar = Calendar.getInstance()
        binding.currentMonthSelector.text = currentMonthFormat.format(currentCalendar.time)

        fetchMoodData(currentCalendar)
    }

    override fun onResume() {
        super.onResume()
        ThemeManager.loadAndApplyTheme(this)
    }

    /**
     * Fetches mood data for the specified month from Firestore.
     * @param calendar The Calendar instance representing the month to display.
     */
    private fun fetchMoodData(calendar: Calendar) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "User not authenticated. Please log in.", Toast.LENGTH_LONG).show()
            return
        }

        // --- 1. Define Month Boundaries using Firestore Timestamp ---
        val startCalendar = calendar.clone() as Calendar
        startCalendar.set(Calendar.DAY_OF_MONTH, 1)
        startCalendar.set(Calendar.HOUR_OF_DAY, 0)
        startCalendar.set(Calendar.MINUTE, 0)
        startCalendar.set(Calendar.SECOND, 0)
        startCalendar.set(Calendar.MILLISECOND, 0)
        val monthStartTimestamp = Timestamp(startCalendar.time)

        val endCalendar = calendar.clone() as Calendar
        endCalendar.set(Calendar.DAY_OF_MONTH, endCalendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        endCalendar.set(Calendar.HOUR_OF_DAY, 23)
        endCalendar.set(Calendar.MINUTE, 59)
        endCalendar.set(Calendar.SECOND, 59)
        endCalendar.set(Calendar.MILLISECOND, 999)
        val monthEndTimestamp = Timestamp(endCalendar.time)

        // Clear previous calendar view before fetching new data
        binding.calendarContainer.removeAllViews()

        // --- 2. Query the data from Firestore with filters ---
        firestoreDb.collection("moodEntries")
            .whereEqualTo("userId", userId)
            .whereGreaterThanOrEqualTo("timestamp", monthStartTimestamp)
            .whereLessThanOrEqualTo("timestamp", monthEndTimestamp)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { result ->

                if (result.isEmpty) {
                    val noDataText = TextView(this).apply {
                        text = "No mood entries found for this month."
                        textSize = 16f
                        gravity = Gravity.CENTER
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            dpToPx(100)
                        )
                        setPadding(0, dpToPx(32), 0, 0)
                    }
                    binding.calendarContainer.addView(noDataText)
                    updateCalendarView(emptyMap(), calendar)
                    return@addOnSuccessListener
                }

                val moodEntries = mutableMapOf<Int, String>() // Day of Month -> Mood (latest for the day)

                result.documents.forEach { doc ->
                    val timestamp = doc.getTimestamp("timestamp")?.toDate()
                    val mood = doc.getString("mood")

                    if (timestamp != null && mood != null) {
                        val cal = Calendar.getInstance().apply { time = timestamp }
                        val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
                        moodEntries[dayOfMonth] = mood
                    }
                }

                updateCalendarView(moodEntries, calendar)

            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error getting mood data: ", exception)
                Toast.makeText(this, "Failed to load calendar data. Check internet/rules.", Toast.LENGTH_LONG).show()
                binding.calendarContainer.removeAllViews()
                val errorText = TextView(this).apply {
                    text = "Failed to load moods. Please check connection."
                    gravity = Gravity.CENTER
                    setPadding(0, dpToPx(32), 0, 0)
                }
                binding.calendarContainer.addView(errorText)
            }
    }

    /**
     * Creates and populates the custom calendar grid inside calendar_container.
     * @param moodData Map of Day of Month (1-31) to Mood String ("Angry", "Happy", etc.)
     * @param currentMonthCalendar The Calendar instance used to determine day count and start day.
     */
    private fun updateCalendarView(moodData: Map<Int, String>, currentMonthCalendar: Calendar) {
        // Clear old grid if it exists
        binding.calendarContainer.removeAllViews()

        val currentDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
        val maxDays = currentMonthCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        val firstDayOfMonth = currentMonthCalendar.clone() as Calendar
        firstDayOfMonth.set(Calendar.DAY_OF_MONTH, 1)
        val offset = firstDayOfMonth.get(Calendar.DAY_OF_WEEK) - 1

        // Removed reliance on minCalendarHeightDp and used WRAP_CONTENT in the XML,
        // so we'll adjust cellHeightPx to ensure a nice, readable square cell size.
        val cellHeightDp = 48
        val cellHeightPx = dpToPx(cellHeightDp)


        val calendarGrid = GridLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            columnCount = 7
        }

        // 2. Add Day Headers (S, M, T, W, T, F, S)
        val daysOfWeek = arrayOf("S", "M", "T", "W", "T", "F", "S")
        daysOfWeek.forEach { day ->
            val dayView = TextView(this).apply {
                text = day
                textSize = 14f
                gravity = Gravity.CENTER
                setPadding(0, dpToPx(8), 0, dpToPx(8))
                layoutParams = GridLayout.LayoutParams(
                    GridLayout.spec(GridLayout.UNDEFINED, 1f),
                    GridLayout.spec(GridLayout.UNDEFINED, 1f)
                ).apply {
                    width = 0
                    height = dpToPx(30)
                }
                val typedValue = TypedValue()
                theme.resolveAttribute(androidx.appcompat.R.attr.colorPrimary, typedValue, true)
                setTextColor(typedValue.data)
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
            val isToday = day == currentDay && currentMonthCalendar.get(Calendar.MONTH) == Calendar.getInstance().get(Calendar.MONTH)

            val dayCell = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4))
                layoutParams = getGridLayoutParams(cellHeightPx)

                // Highlight the current day using a subtle background color from the theme
                if (isToday) {
                    val typedValue = TypedValue()
                    theme.resolveAttribute(androidx.appcompat.R.attr.colorAccent, typedValue, true)
                    val accentColor = typedValue.data
                    setBackgroundColor(Color.argb(50, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor)))
                }
            }

            // Day Number
            val dayNumText = TextView(this).apply {
                text = day.toString()
                textSize = 12f
                gravity = Gravity.CENTER
                if (isToday) {
                    // Make text stand out against the highlighted background
                    setTextColor(Color.BLACK)
                }
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
        val color = when (mood?.lowercase(Locale.getDefault())) {
            "angry" -> Color.parseColor("#E53935") // Red
            "sad" -> Color.parseColor("#42A5F5")   // Blue
            "neutral" -> Color.parseColor("#FFCA28") // Yellow/Orange
            "happy" -> Color.parseColor("#66BB6A") // Green
            "excited" -> Color.parseColor("#7E57C2") // Purple
            else -> Color.TRANSPARENT // Transparent if no mood is logged
        }

        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            if (mood != null) {
                setColor(color)
            }
            setSize(dpToPx(8), dpToPx(8))
        }
    }

    /** Helper for standard GridLayout.LayoutParams, including height */
    private fun getGridLayoutParams(heightPx: Int): GridLayout.LayoutParams {
        return GridLayout.LayoutParams(
            GridLayout.spec(GridLayout.UNDEFINED, 1f),
            GridLayout.spec(GridLayout.UNDEFINED, 1f)
        ).apply {
            width = 0
            height = heightPx
        }
    }

    /** Helper to convert DP to Pixels using activity context resources */
    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            this.resources.displayMetrics
        ).toInt()
    }
}