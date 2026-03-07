package com.example.remme3

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * BootReceiver - מאזין לאתחול המכשיר
 *
 * כשהמשתמש מפעיל את המכשיר מחדש, אנדרואיד מפעיל את ה-BroadcastReceiver הזה.
 * אם המשתמש ביקש מעקב מיקום - מפעילים את השירות אוטומטית.
 *
 * חובה להוסיף לAndroidManifest.xml:
 *   <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
 *
 *   <receiver android:name=".BootReceiver"
 *       android:exported="true">
 *       <intent-filter>
 *           <action android:name="android.intent.action.BOOT_COMPLETED"/>
 *           <action android:name="android.intent.action.QUICKBOOT_POWERON"/>
 *       </intent-filter>
 *   </receiver>
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            val prefs = context.getSharedPreferences("RemMePrefs", Context.MODE_PRIVATE)
            val isEnabled = prefs.getBoolean("location_tracking_enabled", false)

            if (isEnabled) {
                val serviceIntent = Intent(context, LocationTrackingService::class.java)
                context.startForegroundService(serviceIntent)
            }
        }
    }
}