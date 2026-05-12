package com.example.remme3

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import com.google.firebase.auth.FirebaseAuth
import org.json.JSONArray

class MainActivity : BaseActivity() {

    private lateinit var btnLocationTracking: Button
    private var isLocationTrackingEnabled = false
    private val firebaseManager = FirebaseManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        setupMenu()

        btnLocationTracking = findViewById(R.id.Button_on_location)

        val prefs = getSharedPreferences("RemMePrefs", Context.MODE_PRIVATE)
        isLocationTrackingEnabled = prefs.getBoolean("location_tracking_enabled", false)
        updateLocationButton()

        btnLocationTracking.setOnClickListener { toggleLocationTracking() }

        // בקשת הרשאת notifications ב-Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1002
                )
            }
        }

    }

    override fun onResume() {
        super.onResume()

        val prefs = getSharedPreferences("RemMePrefs", Context.MODE_PRIVATE)
        isLocationTrackingEnabled = prefs.getBoolean("location_tracking_enabled", false)
        updateLocationButton()

        // טוען תחילה מ-cache מקומי לתצוגה מיידית, ואז מסנכרן מ-Firestore
        loadItemsDynamic(useCache = true)
        syncItemsFromCloud()
    }

    // ─── סנכרון מ-Firestore ───────────────────────────────────────────────────

    private fun syncItemsFromCloud() {
        firebaseManager.loadItems { cloudItems ->
            runOnUiThread {
                if (cloudItems != null && cloudItems.isNotEmpty()) {
                    // שמור ב-cache המקומי ורענן תצוגה
                    saveItemsToLocal(cloudItems)
                    loadItemsDynamic(useCache = true)
                }
            }
        }
    }

    private fun saveItemsToLocal(items: List<ItemData>) {
        val prefs = getSharedPreferences("RemMePrefs", Context.MODE_PRIVATE)
        val arr = JSONArray()
        items.forEach { item ->
            val obj = org.json.JSONObject().apply {
                put("name", item.name)
                put("icon", item.icon)
                put("isChecked", item.isChecked)
                put("id", item.id)
            }
            arr.put(obj)
        }
        prefs.edit().putString("items_json", arr.toString()).apply()

        val names = items.joinToString(",") { it.name }
        prefs.edit().putString("items_list", names).apply()
    }

    // ─── בניית רשימה דינמית ───────────────────────────────────────────────────

    private fun loadItemsDynamic(useCache: Boolean = true) {
        val container = findViewById<LinearLayout>(R.id.items_dynamic_container) ?: return

        while (container.childCount > 1) {
            container.removeViewAt(1)
        }

        val prefs = getSharedPreferences("RemMePrefs", Context.MODE_PRIVATE)
        val itemsData = loadItemsData(prefs)

        for (item in itemsData) {
            val divider = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 2
                )
                setBackgroundColor(0xFFE0E0E0.toInt())
            }
            container.addView(divider)

            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                val p = (8 * resources.displayMetrics.density).toInt()
                setPadding(0, p, 0, p)
            }

            val iconRes = getIconForItem(item.name)
            if (iconRes != null) {
                val iv = ImageView(this).apply {
                    val size = (32 * resources.displayMetrics.density).toInt()
                    layoutParams = LinearLayout.LayoutParams(size, size)
                    setBackgroundResource(iconRes)
                }
                row.addView(iv)
            } else {
                val tv = TextView(this).apply {
                    val size = (32 * resources.displayMetrics.density).toInt()
                    layoutParams = LinearLayout.LayoutParams(size, size)
                    text = "📦"
                    textSize = 18f
                    gravity = android.view.Gravity.CENTER
                }
                row.addView(tv)
            }

            val nameView = TextView(this).apply {
                val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                lp.marginStart = (12 * resources.displayMetrics.density).toInt()
                layoutParams = lp
                text = item.name
                textSize = 15f
            }
            row.addView(nameView)

            val cb = CheckBox(this).apply {
                isChecked = item.isChecked
                setOnCheckedChangeListener { _, isChecked ->
                    item.isChecked = isChecked
                    // עדכון מהיר - רק הפריט הספציפי ב-Firestore
                    firebaseManager.updateItemChecked(item.id, isChecked)
                    saveCheckedState(prefs, itemsData)
                }
            }
            row.addView(cb)

            container.addView(row)
        }
    }

    // ─── נתונים ──────────────────────────────────────────────────────────────

    private fun loadItemsData(prefs: android.content.SharedPreferences): List<ItemData> {
        val savedJson = prefs.getString("items_json", null)
        if (!savedJson.isNullOrEmpty()) {
            return try {
                val arr = JSONArray(savedJson)
                (0 until arr.length()).map { i ->
                    val obj = arr.getJSONObject(i)
                    ItemData(
                        name = obj.getString("name"),
                        icon = obj.optString("icon", "📦"),
                        isChecked = obj.optBoolean("isChecked", false),
                        id = obj.optString("id", java.util.UUID.randomUUID().toString())
                    )
                }
            } catch (e: Exception) {
                emptyList()
            }
        }

        val oldStr = prefs.getString("items_list", null)
        return if (!oldStr.isNullOrEmpty()) {
            oldStr.split(",").filter { it.isNotEmpty() }.map { ItemData(it, "📦") }
        } else {
            listOf(
                ItemData("מפתחות", "key"),
                ItemData("ארנק", "wallet"),
                ItemData("שעון חכם", "smart_wacth"),
                ItemData("טלפון", "smartphone"),
                ItemData("אוזניות", "headphones")
            )
        }
    }

    private fun saveCheckedState(prefs: android.content.SharedPreferences, items: List<ItemData>) {
        val arr = JSONArray()
        items.forEach { item ->
            val obj = org.json.JSONObject().apply {
                put("name", item.name)
                put("icon", item.icon)
                put("isChecked", item.isChecked)
                put("id", item.id)
            }
            arr.put(obj)
        }
        prefs.edit().putString("items_json", arr.toString()).apply()
    }

    private fun getIconForItem(name: String): Int? = when (name.trim()) {
        "מפתחות" -> R.drawable.key
        "ארנק" -> R.drawable.wallet
        "שעון חכם" -> R.drawable.smart_wacth
        "טלפון" -> R.drawable.smartphone
        "אוזניות" -> R.drawable.headphones
        else -> null
    }

    // ─── מעקב מיקום ──────────────────────────────────────────────────────────

    private fun toggleLocationTracking() {
        isLocationTrackingEnabled = !isLocationTrackingEnabled

        getSharedPreferences("RemMePrefs", Context.MODE_PRIVATE)
            .edit().putBoolean("location_tracking_enabled", isLocationTrackingEnabled).apply()

        updateLocationButton()

        if (isLocationTrackingEnabled) {
            if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                ), 1001)
            } else {
                startLocationService()
            }
        } else {
            stopLocationService()
            Toast.makeText(this, "מעקב מיקום הושבת", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startLocationService() {
        val homeLocation = getSharedPreferences("RemMePrefs", Context.MODE_PRIVATE)
            .getString("home_location", "")

        if (homeLocation.isNullOrEmpty()) {
            Toast.makeText(this, "⚠️ נא להגדיר מיקום בית בהגדרות תחילה", Toast.LENGTH_LONG).show()
            isLocationTrackingEnabled = false
            getSharedPreferences("RemMePrefs", Context.MODE_PRIVATE)
                .edit().putBoolean("location_tracking_enabled", false).apply()
            updateLocationButton()
            return
        }

        startForegroundService(Intent(this, LocationTrackingService::class.java))
        Toast.makeText(this, "מעקב מיקום הופעל! 📍", Toast.LENGTH_SHORT).show()
    }

    private fun stopLocationService() {
        stopService(Intent(this, LocationTrackingService::class.java))
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            1001 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startLocationService()
                } else {
                    Toast.makeText(this, "❌ נדרשת הרשאת מיקום לפעולה זו", Toast.LENGTH_LONG).show()
                    isLocationTrackingEnabled = false
                    updateLocationButton()
                }
            }
        }
    }

    private fun updateLocationButton() {
        if (isLocationTrackingEnabled) {
            btnLocationTracking.text = "השבת מעקב מיקום ❌"
            btnLocationTracking.setBackgroundColor(getColor(android.R.color.holo_red_dark))
        } else {
            btnLocationTracking.text = "הפעל מעקב מיקום ✓"
            btnLocationTracking.setBackgroundColor(getColor(R.color.purple_500))
        }
    }
}