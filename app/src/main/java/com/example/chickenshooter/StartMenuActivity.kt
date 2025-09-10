package com.example.chickenshooter

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

import android.content.Context
import android.graphics.Rect
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import android.widget.EditText

class StartMenuActivity : AppCompatActivity() {

    private var selectedBackground = R.drawable.background
    private var selectedPlane = R.drawable.player

    private lateinit var selectedBgView: ImageView
    private lateinit var selectedPlaneView: ImageView

    private val auth by lazy { Firebase.auth }
    private val prefs by lazy { getSharedPreferences("game", MODE_PRIVATE) }

    // UI (bắt buộc phải có trong layout)
    private lateinit var tvMode: TextView
    private lateinit var tvPlayerName: TextView
    private lateinit var tvCoins: TextView
    private lateinit var etName: EditText
    private lateinit var btnSaveName: Button
    private lateinit var btnLogin: Button

    private var profileListener: ValueEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start_menu)

        val bg1 = findViewById<ImageView>(R.id.bg1Img)
        val bg2 = findViewById<ImageView>(R.id.bg2Img)
        val plane1 = findViewById<ImageView>(R.id.plane1Img)
        val plane2 = findViewById<ImageView>(R.id.plane2Img)

// Khởi tạo mặc định
        selectedBgView = bg1
        selectedPlaneView = plane1
        selectedBgView.setBackgroundResource(R.drawable.bg_selected_border)
        selectedPlaneView.setBackgroundResource(R.drawable.bg_selected_border)

        bg1.setOnClickListener {
            selectedBackground = R.drawable.background
            selectedBgView.background = null
            bg1.setBackgroundResource(R.drawable.bg_selected_border)
            selectedBgView = bg1
        }
        bg2.setOnClickListener {
            selectedBackground = R.drawable.background2
            selectedBgView.background = null
            bg2.setBackgroundResource(R.drawable.bg_selected_border)
            selectedBgView = bg2
        }
        plane1.setOnClickListener {
            selectedPlane = R.drawable.player
            selectedPlaneView.background = null
            plane1.setBackgroundResource(R.drawable.bg_selected_border)
            selectedPlaneView = plane1
        }
        plane2.setOnClickListener {
            selectedPlane = R.drawable.player2_v2
            selectedPlaneView.background = null
            plane2.setBackgroundResource(R.drawable.bg_selected_border)
            selectedPlaneView = plane2
        }

        tvMode = findViewById(R.id.tvMode)
        tvPlayerName = findViewById(R.id.tvPlayerName)
        tvCoins = findViewById(R.id.tvCoins)
        etName = findViewById(R.id.etName)
        btnSaveName = findViewById(R.id.btnSaveName)
        btnLogin = findViewById(R.id.loginBtn)

        btnLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        findViewById<Button>(R.id.startBtn).setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("backgroundId", selectedBackground)
            intent.putExtra("planeId", selectedPlane)
            intent.putExtra("offline_mode", isOfflineMode())
            startActivity(intent)
        }

        btnSaveName.setOnClickListener {
            val inputName = etName.text.toString().trim()
            if (inputName.isEmpty()) {
                toast("Nhập tên trước đã!")
                return@setOnClickListener
            }
            if (isOfflineMode()) {
                setOfflineName(inputName)
                tvPlayerName.text = "Tên: $inputName"
                toast("Đã lưu tên OFFLINE")
            } else {
                val uid = auth.currentUser?.uid
                if (uid == null) {
                    toast("Bạn chưa đăng nhập ONLINE")
                } else {
                    updateDisplayNameRTDB(uid, inputName) {
                        tvPlayerName.text = "Tên: $inputName"
                        toast("Đã lưu tên ONLINE")
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkModeAndLoadProfile()
    }

    override fun onDestroy() {
        super.onDestroy()
        val uid = auth.currentUser?.uid
        if (uid != null && profileListener != null) {
            Firebase.database.getReference("users/$uid")
                .removeEventListener(profileListener!!)
            profileListener = null
        }
    }

    private fun checkModeAndLoadProfile() {
        val offlineMode = isOfflineMode()
        val uid = auth.currentUser?.uid

        tvMode.text = if (offlineMode) "Chế độ: OFFLINE" else "Chế độ: ONLINE"
        btnLogin.isEnabled = !offlineMode
        btnLogin.alpha = if (offlineMode) 0.5f else 1f

        if (offlineMode) {
            val (name, coins) = getOfflineProfile()
            tvPlayerName.text = "Tên: $name"
            tvCoins.text = "Xu: $coins"
            removeProfileListenerIfAny(uid)
        } else {
            if (uid == null) {
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
                return
            }
            loadOnlineProfileRTDB(uid)
        }
    }

    private fun isOfflineMode(): Boolean =
        prefs.getBoolean("offline_mode", false)

    // OFFLINE
    private fun getOfflineProfile(): Pair<String, Long> {
        val name = prefs.getString("display_name", "Player") ?: "Player"
        val coins = prefs.getLong("coins", 0L)
        return name to coins
    }

    private fun setOfflineName(name: String) {
        prefs.edit().putString("display_name", name).apply()
    }

    // ONLINE (RTDB)
    private fun loadOnlineProfileRTDB(uid: String) {
        val database = Firebase.database("https://chickenshooter-bd531-default-rtdb.asia-southeast1.firebasedatabase.app")
        val ref = database.getReference("users/$uid")

        removeProfileListenerIfAny(uid)

        profileListener = object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) {
                val name = s.child("displayName").getValue(String::class.java) ?: "Player"
                val coins = s.child("coins").getValue(Long::class.java) ?: 0L
                tvPlayerName.text = "Tên: $name"
                tvCoins.text = "Xu: $coins"
            }
            override fun onCancelled(error: DatabaseError) {
                toast("Lỗi profile: ${error.message}")
            }
        }
        ref.addValueEventListener(profileListener!!)
    }

    private fun removeProfileListenerIfAny(uid: String?) {
        if (uid != null && profileListener != null) {
            Firebase.database.getReference("users/$uid")
                .removeEventListener(profileListener!!)
            profileListener = null
        }
    }

    private fun updateDisplayNameRTDB(uid: String, name: String, onOk: () -> Unit) {
        val database = Firebase.database("https://chickenshooter-bd531-default-rtdb.asia-southeast1.firebasedatabase.app")
        val ref = database.getReference("users/$uid")
        val updates = mapOf(
            "displayName" to name,
            "updatedAt" to System.currentTimeMillis()
        )
        ref.updateChildren(updates)
            .addOnSuccessListener { onOk() }
            .addOnFailureListener { e -> toast("Lỗi lưu tên: ${e.message}") }
    }

    private fun toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN) {
            val v = currentFocus
            if (v is EditText) {
                val outRect = Rect()
                v.getGlobalVisibleRect(outRect)
                if (!outRect.contains(ev.rawX.toInt(), ev.rawY.toInt())) {
                    v.clearFocus()
                    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(v.windowToken, 0)
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }
}
