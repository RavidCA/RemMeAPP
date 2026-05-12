package com.example.remme3

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import com.google.firebase.auth.FirebaseAuth

/**
 * Activity בסיס שמכיל את התפריט המשותף לכל הדפים
 */
abstract class BaseActivity : AppCompatActivity() {

    private lateinit var btnMenu: ImageView
    private lateinit var menuPanel: LinearLayout
    private lateinit var homeMenuItem: LinearLayout
    private lateinit var itemsMenuItem: LinearLayout
    private lateinit var settingsMenuItem: LinearLayout

    private lateinit var btnLogout: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    protected fun setupMenu() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        try {
            btnMenu = findViewById(R.id.menu)
            menuPanel = findViewById(R.id.menu_panel)
            homeMenuItem = findViewById(R.id.menu_home)
            itemsMenuItem = findViewById(R.id.menu_item)
            settingsMenuItem = findViewById(R.id.menu_setting)
            btnLogout = findViewById(R.id.menu_logout)
            setupMenuListeners()
        } catch (e: Exception) {

        }
    }

    private fun setupMenuListeners() {
        btnMenu.setOnClickListener { toggleMenu() }
        homeMenuItem.setOnClickListener { navigateToHome() }
        itemsMenuItem.setOnClickListener { navigateToItems() }
        settingsMenuItem.setOnClickListener { navigateToSettings() }
        btnLogout.setOnClickListener{ logout() }

    }

    private fun toggleMenu() {
        menuPanel.visibility = if (menuPanel.isVisible) View.GONE else View.VISIBLE
    }

    protected fun closeMenu() {
        if (::menuPanel.isInitialized) {
            menuPanel.visibility = View.GONE
        }
    }

    private fun navigateToHome() {
        if (this !is MainActivity) {
            closeMenu()
            startActivity(Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            })
            finish()
        } else closeMenu()
    }

    private fun navigateToItems() {
        if (this !is ItemsActivity) {
            closeMenu()
            startActivity(Intent(this, ItemsActivity::class.java))
        } else closeMenu()
    }

    private fun navigateToSettings() {
        if (this !is SettingsActivity) {
            closeMenu()
            startActivity(Intent(this, SettingsActivity::class.java))
        } else closeMenu()
    }

    private fun logout() {
        FirebaseAuth.getInstance().signOut()

        // ניקוי נתונים מקומיים (אופציונלי אבל מומלץ)
        getSharedPreferences("RemMePrefs", Context.MODE_PRIVATE)
            .edit().clear().apply()

        // מעבר למסך Login
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)

        finish()
    }

    override fun onResume() {
        super.onResume()
        closeMenu()
    }

    override fun onPause() {
        super.onPause()
        closeMenu()
    }
}