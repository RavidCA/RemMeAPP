package com.example.remme3

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class FirebaseManager {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // ─── AUTH ─────────────────────────────

    fun register(email: String, pass: String, onComplete: (Boolean, String?) -> Unit) {
        auth.createUserWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                onComplete(task.isSuccessful, task.exception?.message)
            }
    }

    fun login(email: String, pass: String, onComplete: (Boolean, String?) -> Unit) {
        auth.signInWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                onComplete(task.isSuccessful, task.exception?.message)
            }
    }

    fun isUserLoggedIn(): Boolean = auth.currentUser != null

    fun getCurrentUserId(): String? = auth.currentUser?.uid

    // ─── ITEMS (תיקייה נפרדת) ─────────────────────────────

    fun saveItems(items: List<ItemData>, onComplete: (Boolean) -> Unit) {
        val uid = getCurrentUserId() ?: run {
            onComplete(false)
            return
        }

        val itemsRef = db.collection("users")
            .document(uid)
            .collection("items")

        val batch = db.batch()

        items.forEach { item ->
            val docRef = itemsRef.document(item.id)

            val data = hashMapOf(
                "name" to item.name,
                "icon" to item.icon,
                "isChecked" to item.isChecked,
                "id" to item.id
            )

            batch.set(docRef, data)
        }

        batch.commit()
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    fun loadItems(onComplete: (List<ItemData>?) -> Unit) {
        val uid = getCurrentUserId() ?: run {
            onComplete(null)
            return
        }

        db.collection("users")
            .document(uid)
            .collection("items")
            .get()
            .addOnSuccessListener { snapshot ->

                val items = snapshot.documents.mapNotNull { doc ->
                    ItemData(
                        name = doc.getString("name") ?: return@mapNotNull null,
                        icon = doc.getString("icon") ?: "📦",
                        isChecked = doc.getBoolean("isChecked") ?: false,
                        id = doc.getString("id") ?: doc.id
                    )
                }

                onComplete(items)
            }
            .addOnFailureListener {
                onComplete(null)
            }
    }

    fun deleteItem(itemId: String) {
        val uid = getCurrentUserId() ?: return

        db.collection("users")
            .document(uid)
            .collection("items")
            .document(itemId)
            .delete()
    }

    fun updateItemChecked(itemId: String, isChecked: Boolean) {
        val uid = getCurrentUserId() ?: return

        db.collection("users")
            .document(uid)
            .collection("items")
            .document(itemId)
            .update("isChecked", isChecked)
    }

    // ─── LOCATION (ברמת USER) ─────────────────────────────

    fun saveHomeLocation(location: String, onComplete: (Boolean) -> Unit) {
        val uid = getCurrentUserId() ?: run {
            onComplete(false)
            return
        }

        db.collection("users")
            .document(uid)
            .update("home_location", location)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener {
                // אם המסמך לא קיים עדיין
                db.collection("users")
                    .document(uid)
                    .set(mapOf("home_location" to location))
                    .addOnSuccessListener { onComplete(true) }
                    .addOnFailureListener { onComplete(false) }
            }
    }

    fun loadHomeLocation(onComplete: (String?) -> Unit) {
        val uid = getCurrentUserId() ?: run {
            onComplete(null)
            return
        }

        db.collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { doc ->
                onComplete(doc.getString("home_location"))
            }
            .addOnFailureListener {
                onComplete(null)
            }
    }
}