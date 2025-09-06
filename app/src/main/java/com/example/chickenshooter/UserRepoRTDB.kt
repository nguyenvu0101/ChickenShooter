package com.example.chickenshooter

import android.content.Context
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class UserRepoRTDB {

    private val prefsName = "game"

    // --- OFFLINE (SharedPreferences) ---
    fun isOffline(ctx: Context): Boolean =
        ctx.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            .getBoolean("offline_mode", false)

    fun getOfflineProfile(ctx: Context): Pair<String, Long> {
        val p = ctx.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val name = p.getString("display_name", "Player") ?: "Player"
        val coins = p.getLong("coins", 0L)
        return name to coins
    }

    fun setOfflineName(ctx: Context, name: String) {
        ctx.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            .edit().putString("display_name", name).apply()
    }

    fun addOfflineCoins(ctx: Context, delta: Long) {
        val p = ctx.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val cur = p.getLong("coins", 0L)
        p.edit().putLong("coins", (cur + delta).coerceAtLeast(0L)).apply()
    }

    // --- ONLINE (Firebase Realtime Database) ---
    fun loadOnlineProfileOnce(
        uid: String,
        onDone: (String, Long) -> Unit,
        onErr: (String) -> Unit = {}
    ) {
        val database = Firebase.database("https://chickenshooter-bd531-default-rtdb.asia-southeast1.firebasedatabase.app")
        val ref = database.getReference("users/$uid")
        ref.get()
            .addOnSuccessListener { s ->
                val name = s.child("displayName").getValue(String::class.java) ?: "Player"
                val coins = s.child("coins").getValue(Long::class.java) ?: 0L
                onDone(name, coins)
            }
            .addOnFailureListener { e -> onErr(e.message ?: "unknown") }
    }

    fun updateDisplayName(
        uid: String,
        name: String,
        onOk: () -> Unit = {},
        onErr: (String) -> Unit = {}
    ) {
        val database = Firebase.database("https://chickenshooter-bd531-default-rtdb.asia-southeast1.firebasedatabase.app")
        val ref = database.getReference("users/$uid")
        ref.updateChildren(
            mapOf(
                "displayName" to name,
                "updatedAt" to System.currentTimeMillis()
            )
        ).addOnSuccessListener { onOk() }
            .addOnFailureListener { e -> onErr(e.message ?: "unknown") }
    }
    fun setCoins(
        uid: String,
        coins: Long,
        onOk: () -> Unit = {},
        onErr: (String) -> Unit = {}
    ) {
        val ref = Firebase.database.getReference("users/$uid/coins")
        ref.setValue(coins)
            .addOnSuccessListener { onOk() }
            .addOnFailureListener { e -> onErr(e.message ?: "unknown") }
    }
    fun addCoins(
        uid: String,
        delta: Long,
        onOk: () -> Unit = {},
        onErr: (String) -> Unit = {}
    ) {
        val ref = Firebase.database.getReference("users/$uid/coins")
        ref.runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val cur = currentData.getValue(Long::class.java) ?: 0L
                currentData.value = (cur + delta).coerceAtLeast(0L)
                return Transaction.success(currentData)
            }

            override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {
                if (error != null) onErr(error.message ?: "unknown") else onOk()
            }
        })
    }
}
