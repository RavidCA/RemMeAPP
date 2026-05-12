package com.example.remme3

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.json.JSONArray
import org.json.JSONObject

class ItemsActivity : BaseActivity() {

    private lateinit var fabAddItem: FloatingActionButton
    private lateinit var itemsListView: ListView
    private val itemsList = mutableListOf<ItemData>()
    private lateinit var adapter: ItemsAdapter
    private val firebaseManager = FirebaseManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_items)
        setupMenu()

        fabAddItem = findViewById(R.id.fab_add_item)
        itemsListView = findViewById(R.id.items_listview)

        adapter = ItemsAdapter(this, itemsList,
            onDelete = { position -> deleteItem(position) },
            onCheckedChange = { position, checked ->
                itemsList[position].isChecked = checked
                // עדכון מהיר של פריט בודד ב-Firestore (בלי לכתוב את כל הרשימה)
                firebaseManager.updateItemChecked(itemsList[position].id, checked)
                saveItemsLocally()
            }
        )
        itemsListView.adapter = adapter

        loadItems()
        fabAddItem.setOnClickListener { showAddItemDialog() }
    }

    // ─── טעינה ───────────────────────────────────────────────────────────────

    /**
     * טוען קודם מ-SharedPreferences (מהיר) ואז מסנכרן מ-Firestore.
     */
    private fun loadItems() {
        loadItemsLocally()   // תצוגה מיידית

        // סנכרון מ-Firestore ברקע
        firebaseManager.loadItems { cloudItems ->
            runOnUiThread {
                if (cloudItems != null && cloudItems.isNotEmpty()) {
                    itemsList.clear()
                    itemsList.addAll(cloudItems)
                    adapter.notifyDataSetChanged()
                    saveItemsLocally()   // עדכן cache מקומי
                } else if (cloudItems != null && cloudItems.isEmpty() && itemsList.isNotEmpty()) {
                    // משתמש חדש - שמור את ברירת המחדל ב-Firestore
                    saveItemsToCloud()
                }
                updateEmptyMessage()
            }
        }
    }

    private fun loadItemsLocally() {
        val prefs = getSharedPreferences("RemMePrefs", Context.MODE_PRIVATE)
        val savedJson = prefs.getString("items_json", null)
        itemsList.clear()

        if (savedJson.isNullOrEmpty()) {
            itemsList.addAll(defaultItems())
            saveItemsLocally()
        } else {
            try {
                val arr = JSONArray(savedJson)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    itemsList.add(ItemData(
                        name = obj.getString("name"),
                        icon = obj.optString("icon", "📦"),
                        isChecked = obj.optBoolean("isChecked", false),
                        id = obj.optString("id", java.util.UUID.randomUUID().toString())
                    ))
                }
            } catch (e: Exception) {
                itemsList.addAll(defaultItems())
            }
        }

        adapter.notifyDataSetChanged()
        updateEmptyMessage()
    }

    private fun defaultItems() = listOf(
        ItemData("מפתחות", "key"),
        ItemData("ארנק", "wallet"),
        ItemData("שעון חכם", "smart_wacth"),
        ItemData("טלפון", "smartphone"),
        ItemData("אוזניות", "headphones")
    )

    // ─── שמירה ───────────────────────────────────────────────────────────────

    fun saveItemsToStorage() {
        saveItemsLocally()
        saveItemsToCloud()
    }

    private fun saveItemsToCloud() {
        firebaseManager.saveItems(itemsList) { /* אפשר להוסיף Toast אם רוצים */ }
    }

    fun saveItemsLocally() {
        val prefs = getSharedPreferences("RemMePrefs", Context.MODE_PRIVATE)
        val arr = JSONArray()
        itemsList.forEach { item ->
            val obj = JSONObject().apply {
                put("name", item.name)
                put("icon", item.icon)
                put("isChecked", item.isChecked)
                put("id", item.id)
            }
            arr.put(obj)
        }
        prefs.edit().putString("items_json", arr.toString()).apply()

        // שמירת שמות בלבד עבור LocationTrackingService
        val names = itemsList.joinToString(",") { it.name }
        prefs.edit().putString("items_list", names).apply()
    }

    // ─── UI ──────────────────────────────────────────────────────────────────

    private fun updateEmptyMessage() {
        val emptyMessage = findViewById<TextView>(R.id.emptyMessage)
        emptyMessage.visibility = if (itemsList.isEmpty()) View.VISIBLE else View.GONE
        itemsListView.visibility = if (itemsList.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun showAddItemDialog() {
        val editText = EditText(this).apply {
            hint = "שם הפריט (לדוגמה: משקפיים)"
            setPadding(50, 40, 50, 40)
            textSize = 16f
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            textDirection = View.TEXT_DIRECTION_RTL
            inputType = android.text.InputType.TYPE_CLASS_TEXT
        }

        AlertDialog.Builder(this)
            .setTitle("➕ הוספת פריט חדש")
            .setMessage("הזן את שם הפריט שברצונך להוסיף:")
            .setView(editText)
            .setPositiveButton("הוסף") { _, _ ->
                val name = editText.text.toString().trim()
                if (name.isNotEmpty()) addItem(name)
                else Toast.makeText(this, "נא להזין שם פריט", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("ביטול", null)
            .show()
        editText.requestFocus()
    }

    private fun addItem(itemName: String) {
        itemsList.add(ItemData(itemName, "📦"))
        adapter.notifyDataSetChanged()
        saveItemsToStorage()
        updateEmptyMessage()
        Toast.makeText(this, "✓ נוסף: $itemName", Toast.LENGTH_SHORT).show()
    }

    private fun deleteItem(position: Int) {
        val item = itemsList[position]

        AlertDialog.Builder(this)
            .setTitle("🗑️ מחיקת פריט")
            .setMessage("האם למחוק את '${item.name}'?")
            .setPositiveButton("מחק") { _, _ ->

                // 🔥 מחיקה מ-Firebase
                firebaseManager.deleteItem(item.id)

                // 🔥 מחיקה מקומית
                itemsList.removeAt(position)
                adapter.notifyDataSetChanged()

                saveItemsLocally()
                updateEmptyMessage()

                Toast.makeText(this, "נמחק: ${item.name}", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("ביטול", null)
            .show()
    }
}

// ─── Adapter ─────────────────────────────────────────────────────────────────

class ItemsAdapter(
    private val context: Context,
    private val items: List<ItemData>,
    private val onDelete: (Int) -> Unit,
    private val onCheckedChange: (Int, Boolean) -> Unit
) : BaseAdapter() {

    override fun getCount() = items.size
    override fun getItem(position: Int) = items[position]
    override fun getItemId(position: Int) = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val item = items[position]
        val density = context.resources.displayMetrics.density

        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            val p = (4 * density).toInt()
            setPadding(16, p, 16, p)
            gravity = android.view.Gravity.CENTER_VERTICAL
        }

        val iconView = TextView(context).apply {
            val size = (32 * density).toInt()
            layoutParams = LinearLayout.LayoutParams(size, size)
            text = "📦"
            textSize = 18f
            gravity = android.view.Gravity.CENTER
        }

        val nameView = TextView(context).apply {
            val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            lp.marginStart = (12 * density).toInt()
            layoutParams = lp
            text = item.name
            textSize = 16f
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }

        val checkBox = CheckBox(context).apply {
            isChecked = item.isChecked
            setOnCheckedChangeListener { _, isChecked ->
                onCheckedChange(position, isChecked)
            }
        }

        val deleteBtn = Button(context).apply {
            text = "🗑️"
            textSize = 16f
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            setOnClickListener { onDelete(position) }
        }

        row.addView(iconView)
        row.addView(nameView)
        row.addView(checkBox)
        row.addView(deleteBtn)

        return row
    }
}