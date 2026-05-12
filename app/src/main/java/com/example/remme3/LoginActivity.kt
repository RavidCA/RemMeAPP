package com.example.remme3

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {

    private lateinit var firebaseManager: FirebaseManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        firebaseManager = FirebaseManager()

        // אם המשתמש כבר מחובר - דלג ישר למסך הראשי
        if (firebaseManager.isUserLoggedIn()) {
            goToMain()
            return
        }

        val etEmail = findViewById<EditText>(R.id.et_email)
        val etPassword = findViewById<EditText>(R.id.et_password)
        val btnLogin = findViewById<Button>(R.id.btn_login)
        val btnRegister = findViewById<Button>(R.id.btn_register)

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val pass = etPassword.text.toString()
            if (email.isNotEmpty() && pass.isNotEmpty()) {
                firebaseManager.login(email, pass) { success, error ->
                    runOnUiThread {
                        if (success) goToMain()
                        else Toast.makeText(this, "שגיאה: $error", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "נא למלא אימייל וסיסמה", Toast.LENGTH_SHORT).show()
            }
        }

        btnRegister.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val pass = etPassword.text.toString()
            if (email.isNotEmpty() && pass.length >= 6) {
                firebaseManager.register(email, pass) { success, error ->
                    runOnUiThread {
                        if (success) {
                            Toast.makeText(this, "נרשמת בהצלחה!", Toast.LENGTH_SHORT).show()
                            goToMain()
                        } else {
                            Toast.makeText(this, "שגיאה: $error", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } else {
                Toast.makeText(this, "סיסמה חייבת להיות לפחות 6 תווים", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}