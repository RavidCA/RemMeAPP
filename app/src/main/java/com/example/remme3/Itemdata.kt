package com.example.remme3

/**
 * מחלקת נתונים לפריט ברשימה
 * id משמש לשמירה ב-Firebase בעתיד
 */
data class ItemData(
    val name: String,
    val icon: String,
    var isChecked: Boolean = false,
    var id: String = java.util.UUID.randomUUID().toString()
)