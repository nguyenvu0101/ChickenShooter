package com.example.chickenshooter

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

import android.content.Context
import android.graphics.Rect
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.appcompat.app.AlertDialog

class StartMenuActivity : AppCompatActivity() {

    private var selectedPlane = R.drawable.player

    private lateinit var selectedPlaneView: ImageView

    private val prefs by lazy { getSharedPreferences("game", MODE_PRIVATE) }

    // UI
    private lateinit var tvPlayerName: TextView
    private lateinit var tvCoins: TextView
    private lateinit var etName: EditText
    private lateinit var btnSaveName: Button
    private lateinit var btnChangeName: Button
    private lateinit var btnCart: ImageButton // Giỏ hàng
    private lateinit var btnLeaderboard: ImageButton // Bảng xếp hạng

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start_menu)

        prefs.edit().putString("current_plane", "player").apply()

        val plane1 = findViewById<ImageView>(R.id.plane1Img)
        val plane2 = findViewById<ImageView>(R.id.plane2Img)

        // Tham chiếu giỏ hàng và bxh
        btnCart = findViewById(R.id.btnCart)
        btnLeaderboard = findViewById(R.id.btnLeaderboard)

        // Bắt sự kiện mở bảng xếp hạng
        btnLeaderboard.setOnClickListener {
            val intent = Intent(this, LeaderboardActivity::class.java)
            startActivity(intent)
        }

        // Khởi tạo mặc định
        selectedPlaneView = plane1
        selectedPlaneView.setBackgroundResource(R.drawable.bg_selected_border)
        plane1.setOnClickListener {
            selectedPlane = R.drawable.player
            selectedPlaneView.background = null
            plane1.setBackgroundResource(R.drawable.bg_selected_border)
            selectedPlaneView = plane1
            prefs.edit().putString("current_plane", "player").apply()
        }
        plane2.setOnClickListener {
            selectedPlane = R.drawable.player2_v2
            selectedPlaneView.background = null
            plane2.setBackgroundResource(R.drawable.bg_selected_border)
            selectedPlaneView = plane2
            prefs.edit().putString("current_plane", "player2_v2").apply()
        }

        tvPlayerName = findViewById(R.id.tvPlayerName)
        tvCoins = findViewById(R.id.tvCoins)
        etName = findViewById(R.id.etName)
        btnSaveName = findViewById(R.id.btnSaveName)
        btnChangeName = findViewById(R.id.btnChangeName)

        findViewById<Button>(R.id.startBtn).setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("planeId", selectedPlane)
            startActivity(intent)
        }

        btnSaveName.setOnClickListener {
            val inputName = etName.text.toString().trim()
            if (inputName.isEmpty()) {
                toast("Nhập tên trước đã!")
                return@setOnClickListener
            }
            setOfflineName(inputName)
            tvPlayerName.text = "Tên: $inputName"
            
            // Ẩn form nhập tên và hiển thị tên đã lưu
            etName.visibility = View.GONE
            btnSaveName.visibility = View.GONE
            tvPlayerName.visibility = View.VISIBLE
            btnChangeName.visibility = View.VISIBLE
            
            toast("Đã lưu tên")
        }
        
        btnChangeName.setOnClickListener {
            // Hiển thị form nhập tên để đổi tên
            etName.visibility = View.VISIBLE
            btnSaveName.visibility = View.VISIBLE
            tvPlayerName.visibility = View.GONE
            btnChangeName.visibility = View.GONE
            etName.setText("")
            etName.requestFocus()
        }

        // Sự kiện mở giỏ hàng
        btnCart.setOnClickListener {
            val intent = Intent(this, CartActivity::class.java)
            startActivity(intent)
        }
    }

    private fun getCurrentCoins(): Long {
        return prefs.getLong("coins", 0L)
    }

    private fun setCurrentCoins(coins: Long) {
        prefs.edit().putLong("coins", coins).apply()
    }

    override fun onResume() {
        super.onResume()
        loadOfflineProfile()
        updateSelectedPlaneFromPrefs()
    }

    private fun loadOfflineProfile() {
        val (name, coins) = getOfflineProfile()
        tvPlayerName.text = "Tên: $name"
        tvCoins.text = "Xu: $coins"
        
        // Kiểm tra xem đã có tên chưa
        val hasName = prefs.getString("display_name", null) != null
        if (hasName) {
            // Đã có tên, ẩn form nhập tên và hiển thị nút đổi tên
            etName.visibility = View.GONE
            btnSaveName.visibility = View.GONE
            tvPlayerName.visibility = View.VISIBLE
            btnChangeName.visibility = View.VISIBLE
        } else {
            // Chưa có tên, hiển thị form nhập tên
            etName.visibility = View.VISIBLE
            btnSaveName.visibility = View.VISIBLE
            tvPlayerName.visibility = View.GONE
            btnChangeName.visibility = View.GONE
        }
    }

    // OFFLINE
    private fun getOfflineProfile(): Pair<String, Long> {
        val name = prefs.getString("display_name", "Player") ?: "Player"
        val coins = prefs.getLong("coins", 0L)
        return name to coins
    }

    private fun setOfflineName(name: String) {
        prefs.edit().putString("display_name", name).apply()
    }

    // Cập nhật máy bay đang chọn từ SharedPreferences khi quay lại menu
    private fun updateSelectedPlaneFromPrefs() {
        val currentPlaneId = prefs.getString("current_plane", "player")
        val plane1 = findViewById<ImageView>(R.id.plane1Img)
        val plane2 = findViewById<ImageView>(R.id.plane2Img)
        val planePreview = findViewById<ImageView>(R.id.planePreviewImg)
        val planeSelectLayout = findViewById<LinearLayout>(R.id.planeSelectLayout)

        // Reset viền chọn
        plane1.background = null
        plane2.background = null

        // Nếu chọn máy bay đặc biệt
        if (currentPlaneId == "player_cart1") {
            planePreview.setImageResource(R.drawable.player_cart1)
            planePreview.visibility = View.VISIBLE
            planeSelectLayout.visibility = View.GONE
            // FIX: cập nhật biến selectedPlane
            selectedPlane = R.drawable.player_cart1
            return
        }
        if (currentPlaneId == "player_cart2") {
            planePreview.setImageResource(R.drawable.player_cart2)
            planePreview.visibility = View.VISIBLE
            planeSelectLayout.visibility = View.GONE
            selectedPlane = R.drawable.player_cart2
            return
        }

        // Nếu là máy bay mặc định, hiện 2 máy bay nhỏ, ẩn máy bay lớn
        planePreview.visibility = View.GONE
        planeSelectLayout.visibility = View.VISIBLE

        if (currentPlaneId == "player2_v2") {
            plane2.setBackgroundResource(R.drawable.bg_selected_border)
            selectedPlane = R.drawable.player2_v2
        } else {
            plane1.setBackgroundResource(R.drawable.bg_selected_border)
            selectedPlane = R.drawable.player
        }
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