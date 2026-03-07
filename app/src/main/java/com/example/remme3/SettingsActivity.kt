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
        val homeLocation = prefs.getString("home_location", "")
        alertDistance = prefs.getInt("alert_distance", 100)

        if (!homeLocation.isNullOrEmpty()) {
            editHomeLocation.setText(homeLocation)
        }
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
            // ✅ מנקה קואורדינטות ישנות כדי שה-Service יבצע geocoding מחדש
            .remove("home_latitude")
            .remove("home_longitude")
            .apply()

        Toast.makeText(this, "✓ מיקום בית נשמר: $locationName", Toast.LENGTH_LONG).show()
    }

    private fun saveAllSettings() {
        val prefs = getSharedPreferences("RemMePrefs", Context.MODE_PRIVATE)
        val location = editHomeLocation.text.toString().trim()

        val editor = prefs.edit()
        if (location.isNotEmpty()) {
            editor.putString("home_location", location)
            // ✅ מנקה קואורדינטות ישנות לגיאוקודינג מחדש
            editor.remove("home_latitude")
            editor.remove("home_longitude")
        }
        editor.putInt("alert_distance", alertDistance)
        editor.apply()

        val distanceText = if (alertDistance >= 1000)
            String.format("%.1f ק\"מ", alertDistance / 1000.0)
        else "$alertDistance מטר"

        Toast.makeText(
            this,
            "✓ ההגדרות נשמרו!\n📍 מיקום: ${if (location.isEmpty()) "לא הוגדר" else location}\n📏 מרחק: $distanceText",
            Toast.LENGTH_LONG
        ).show()
    }
}

data class UserSettings(
    var homeLocation: String = "",
    var homeLatitude: Double = 0.0,
    var homeLongitude: Double = 0.0,
    var alertDistance: Int = 100,
    var isLocationTrackingEnabled: Boolean = false
)