package com.example.chickenshooter

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import android.view.View

class StartMenuActivity : AppCompatActivity() {

    // Danh sách máy bay, mỗi máy bay có 2 ảnh: menu (góc chéo), game (góc thẳng)
    private val planes = listOf(
        Plane(R.drawable.player_demo, R.drawable.player),
        Plane(R.drawable.player3_demo, R.drawable.player3
        ),
        // Thêm máy bay khác nếu có (R.drawable.player3_menu, R.drawable.player3_game), ...
    )

    private var selectedIndex = 0 // máy bay đang chọn

    private val prefs by lazy { getSharedPreferences("game", MODE_PRIVATE) }

    // UI
    private lateinit var tvPlayerName: TextView
    private lateinit var tvCoins: TextView
    private lateinit var etName: EditText
    private lateinit var btnSaveName: Button
    private lateinit var btnChangeName: Button
    private lateinit var btnCart: ImageButton
    private lateinit var btnLeaderboard: ImageButton
    private lateinit var planeViewPager: ViewPager2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start_menu)

        // Khởi tạo ViewPager2 chọn máy bay (swipe trái/phải)
        planeViewPager = findViewById(R.id.planeViewPager)
        planeViewPager.adapter = PlanePagerAdapter(planes)
        planeViewPager.setCurrentItem(selectedIndex, false)
        planeViewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                selectedIndex = position
                prefs.edit().putInt("current_plane_index", selectedIndex).apply()
            }
        })

        // Tham chiếu UI
        tvPlayerName = findViewById(R.id.tvPlayerName)
        tvCoins = findViewById(R.id.tvCoins)
        etName = findViewById(R.id.etName)
        btnSaveName = findViewById(R.id.btnSaveName)
        btnChangeName = findViewById(R.id.btnChangeName)
        btnCart = findViewById(R.id.btnCart)
        btnLeaderboard = findViewById(R.id.btnLeaderboard)

        btnLeaderboard.setOnClickListener {
            val intent = Intent(this, LeaderboardActivity::class.java)
            startActivity(intent)
        }
        btnCart.setOnClickListener {
            val intent = Intent(this, CartActivity::class.java)
            startActivity(intent)
        }

        findViewById<Button>(R.id.startBtn).setOnClickListener {
            val selectedPlane = planes[selectedIndex]
            // Truyền vào MainActivity đúng ảnh máy bay góc thẳng
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("planeGameResId", selectedPlane.gameResId)
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
            etName.visibility = View.GONE
            btnSaveName.visibility = View.GONE
            tvPlayerName.visibility = View.VISIBLE
            btnChangeName.visibility = View.VISIBLE
            toast("Đã lưu tên")
        }

        btnChangeName.setOnClickListener {
            etName.visibility = View.VISIBLE
            btnSaveName.visibility = View.VISIBLE
            tvPlayerName.visibility = View.GONE
            btnChangeName.visibility = View.GONE
            etName.setText("")
            etName.requestFocus()
        }
    }

    override fun onResume() {
        super.onResume()
        loadOfflineProfile()
        // Cập nhật máy bay đã chọn từ prefs (nếu quay lại menu)
        selectedIndex = prefs.getInt("current_plane_index", 0)
        planeViewPager.setCurrentItem(selectedIndex, false)
    }

    private fun getCurrentCoins(): Long {
        return prefs.getLong("coins", 0L)
    }

    private fun setCurrentCoins(coins: Long) {
        prefs.edit().putLong("coins", coins).apply()
    }

    private fun loadOfflineProfile() {
        val (name, coins) = getOfflineProfile()
        tvPlayerName.text = "Tên: $name"
        tvCoins.text = "Xu: $coins"
        val hasName = prefs.getString("display_name", null) != null
        if (hasName) {
            etName.visibility = View.GONE
            btnSaveName.visibility = View.GONE
            tvPlayerName.visibility = View.VISIBLE
            btnChangeName.visibility = View.VISIBLE
        } else {
            etName.visibility = View.VISIBLE
            btnSaveName.visibility = View.VISIBLE
            tvPlayerName.visibility = View.GONE
            btnChangeName.visibility = View.GONE
        }
    }

    private fun getOfflineProfile(): Pair<String, Long> {
        val name = prefs.getString("display_name", "Player") ?: "Player"
        val coins = prefs.getLong("coins", 0L)
        return name to coins
    }

    private fun setOfflineName(name: String) {
        prefs.edit().putString("display_name", name).apply()
    }

    private fun toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}