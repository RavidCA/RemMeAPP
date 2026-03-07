package com.example.remme3

import android.app.*
import android.content.Context
import android.content.Intent
import android.location.Geocoder
import android.location.Location
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import java.util.Locale

/**
 * LocationTrackingService - Foreground Service למעקב מיקום
 *
 * שיפורים מהגרסה הקודמת:
 * 1. START_STICKY - Android יאתחל את השירות אם יהרוס אותו
 * 2. Cooldown של 5 דקות בין התראות - לא ישלח ספאם
 * 3. טעינה מחדש של הגדרות בכל עדכון מיקום (מרחק/כתובת עלולים להשתנות)
 * 4. geocoding ב-Thread נפרד (לא על Main Thread)
 * 5. הגנה על SecurityException בצורה נכונה
 */
class LocationTrackingService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private var homeLatLng: Pair<Double, Double>? = null
    private var alertDistance = 100
    private var lastAlertTime = 0L
    private val ALERT_COOLDOWN_MS = 5 * 60 * 1000L  // 5 דקות בין התראות

    // ✅ חדש: עקוב אחרי המיקום הקודם לזיהוי "יציאה מהבית" ולא רק "מחוץ לבית"
    private var wasAtHome = true

    companion object {
        const val CHANNEL_ID = "location_tracking_channel"
        const val NOTIFICATION_ID = 1
        const val ALERT_NOTIFICATION_ID = 2
        const val ACTION_STOP = "com.example.remme3.STOP_SERVICE"
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        loadSettings()
        startForeground(NOTIFICATION_ID, buildForegroundNotification())
        startLocationUpdates()

        // ✅ START_STICKY: Android יאתחל את השירות אוטומטית אם יהרוס אותו
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun loadSettings() {
        val prefs = getSharedPreferences("RemMePrefs", Context.MODE_PRIVATE)
        alertDistance = prefs.getInt("alert_distance", 100)

        val savedLat = prefs.getFloat("home_latitude", 0f).toDouble()
        val savedLng = prefs.getFloat("home_longitude", 0f).toDouble()

        if (savedLat != 0.0 && savedLng != 0.0) {
            homeLatLng = Pair(savedLat, savedLng)
        } else {
            val homeAddress = prefs.getString("home_location", "") ?: ""
            if (homeAddress.isNotEmpty()) {
                // ✅ תוקן: geocoding ב-Thread נפרד ולא על Main Thread
                Thread { geocodeAddress(homeAddress) }.start()
            }
        }
    }

    private fun geocodeAddress(address: String) {
        try {
            val geocoder = Geocoder(this, Locale.getDefault())
            val results = geocoder.getFromLocationName(address, 1)
            if (!results.isNullOrEmpty()) {
                val location = results[0]
                homeLatLng = Pair(location.latitude, location.longitude)

                getSharedPreferences("RemMePrefs", Context.MODE_PRIVATE).edit()
                    .putFloat("home_latitude", location.latitude.toFloat())
                    .putFloat("home_longitude", location.longitude.toFloat())
                    .apply()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startLocationUpdates() {
        // ✅ תוקן: עדכון כל 30 שניות, minimum interval 15 שניות
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 30_000L
        )
            .setMinUpdateIntervalMillis(15_000L)
            .setWaitForAccurateLocation(false)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                // ✅ טעינה מחדש של הגדרות בכל פעם - אם המשתמש שינה מרחק/כתובת זה ייכנס לתוקף
                loadSettings()
                checkIfLeftHome(location)
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest, locationCallback, Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            stopSelf()
        }
    }

    private fun checkIfLeftHome(currentLocation: Location) {
        val home = homeLatLng ?: return

        val homeLocation = Location("home").apply {
            latitude = home.first
            longitude = home.second
        }

        val distanceMeters = currentLocation.distanceTo(homeLocation)
        val isAtHome = distanceMeters <= alertDistance

        if (isAtHome) {
            // חזרנו הביתה - מאפסים
            wasAtHome = true
        } else {
            // מחוץ לבית
            if (wasAtHome) {
                // ✅ זה רגע היציאה מהבית! שלח התראה
                wasAtHome = false
                val now = System.currentTimeMillis()
                if (now - lastAlertTime > ALERT_COOLDOWN_MS) {
                    lastAlertTime = now
                    sendForgotItemsAlert(distanceMeters.toInt())
                }
            }
            // אם כבר היינו מחוץ לבית - לא שולחים שוב
        }
    }

    private fun sendForgotItemsAlert(distance: Int) {
        val prefs = getSharedPreferences("RemMePrefs", Context.MODE_PRIVATE)
        val savedItems = prefs.getString("items_list", "מפתחות,ארנק,טלפון")
        val itemsList = savedItems?.split(",")?.filter { it.isNotEmpty() }?.take(5)
            ?.joinToString(", ") ?: "פריטים"

        // Intent לפתיחת האפליקציה בלחיצה על ההתראה
        val openAppIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("🚨 שכחת משהו?")
            .setContentText("יצאת מהבית! בדוק: $itemsList")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("יצאת ${distance} מטר מהבית!\n\nאל תשכח לקחת:\n$itemsList")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .setContentIntent(openAppIntent)
            .build()

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(ALERT_NOTIFICATION_ID, notification)
    }

    private fun buildForegroundNotification(): Notification {
        // ✅ כפתור עצירה ישירות מההתראה
        val stopIntent = PendingIntent.getService(
            this, 0,
            Intent(this, LocationTrackingService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("RemMe פעיל 📍")
            .setContentText("שומר שלא תשכח כלום. מרחק התראה: ${alertDistance}מ'")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(android.R.drawable.ic_delete, "עצור", stopIntent)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "מעקב מיקום",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "התראות שכחת פריטים"
            enableVibration(true)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }
}