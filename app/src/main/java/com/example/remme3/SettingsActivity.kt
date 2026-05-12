package com.example.remme3

import android.content.Context
import android.os.Bundle
import android.widget.*
import androidx.activity.enableEdgeToEdge

class SettingsActivity : BaseActivity() {

    private lateinit var editHomeLocation: EditText
    private lateinit var btnSetLocation: Button
    private lateinit var seekBarDistance: SeekBar
    private lateinit var tvDistanceValue: TextView
    private lateinit var btnSaveSettings: Button
    private val firebaseManager = FirebaseManager()

    private var alertDistance = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings)
        setupMenu()
        initViews()
        loadSettings()
    }

    private fun initViews() {
        editHomeLocation = findViewById(R.id.edit_home_location)
        btnSetLocation = findViewById(R.id.btn_set_location)
        seekBarDistance = findViewById(R.id.seekbar_distance)
        tvDistanceValue = findViewById(R.id.tv_distance_value)
        btnSaveSettings = findViewById(R.id.btn_save_settings)

        seekBarDistance.max = 2000
        seekBarDistance.min = 50
        seekBarDistance.progress = alertDistance

        seekBarDistance.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                alertDistance = progress
                updateDistanceText()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        btnSetLocation.setOnClickListener {
            val location = editHomeLocation.text.toString().trim()
            setHomeLocation(location)
        }

        btnSaveSettings.setOnClickListener { saveAllSettings() }

        updateDistanceText()
    }

    private fun updateDistanceText() {
        tvDistanceValue.text = if (alertDistance >= 1000) {
            String.format("%.1f ק\"מ", alertDistance / 1000.0)
        } else {
            "$alertDistance מטר"
        }
    }

    private fun loadSettings() {

        val prefs = getSharedPreferences("RemMePrefs", Context.MODE_PRIVATE)

        // קודם נטען מהענן ☁️
        firebaseManager.loadHomeLocation { location ->

            runOnUiThread {

                if (!location.isNullOrEmpty()) {
                    editHomeLocation.setText(location)

                    // נשמור גם ל-local cache
                    prefs.edit()
                        .putString("home_location", location)
                        .apply()
                } else {
                    // fallback ללוקאל אם אין ענן
                    val local = prefs.getString("home_location", "")
                    if (!local.isNullOrEmpty()) {
                        editHomeLocation.setText(local)
                    }
                }
            }
        }

        // עדיין טוען את המרחק מהלוקאל
        alertDistance = prefs.getInt("alert_distance", 100)
        seekBarDistance.progress = alertDistance
        updateDistanceText()
    }

    private fun setHomeLocation(locationName: String) {
        if (locationName.isEmpty()) {
            Toast.makeText(this, "❌ נא להזין כתובת", Toast.LENGTH_SHORT).show()
            return
        }

        val prefs = getSharedPreferences("RemMePrefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("home_location", locationName)
            .remove("home_latitude")
            .remove("home_longitude")
            .apply()

        firebaseManager.saveHomeLocation(locationName) { success ->
            runOnUiThread {
                if (success) {
                    Toast.makeText(this, "✓ נשמר בענן ☁️", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "❌ שגיאה בשמירה בענן", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun saveAllSettings() {
        val prefs = getSharedPreferences("RemMePrefs", Context.MODE_PRIVATE)
        val location = editHomeLocation.text.toString().trim()

        val editor = prefs.edit()

        if (location.isNotEmpty()) {
            editor.putString("home_location", location)
            editor.remove("home_latitude")
            editor.remove("home_longitude")

            // 🔥 שמירה ל-Firebase רק אם יש ערך אמיתי
            firebaseManager.saveHomeLocation(location) { }
        }

        editor.putInt("alert_distance", alertDistance)
        editor.apply()

        Toast.makeText(this, "✓ ההגדרות נשמרו!", Toast.LENGTH_LONG).show()
    }


data class UserSettings(
    var homeLocation: String = "",
    var homeLatitude: Double = 0.0,
    var homeLongitude: Double = 0.0,
    var alertDistance: Int = 100,
    var isLocationTrackingEnabled: Boolean = false
)
}