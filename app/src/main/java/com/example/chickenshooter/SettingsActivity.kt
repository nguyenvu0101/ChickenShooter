package com.example.chickenshooter

import android.content.Context
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    private lateinit var etPlayerName: EditText
    private lateinit var btnSaveName: Button
    private lateinit var btnBack: Button
    private lateinit var tvCurrentName: TextView
    private lateinit var tvCoins: TextView

    private val prefs by lazy { getSharedPreferences("game", Context.MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        etPlayerName = findViewById(R.id.etPlayerName)
        btnSaveName = findViewById(R.id.btnSaveName)
        btnBack = findViewById(R.id.btnBack)
        tvCurrentName = findViewById(R.id.tvCurrentName)
        tvCoins = findViewById(R.id.tvCoins)

        // Load current player name and coins
        val currentName = prefs.getString("player_name", "Player") ?: "Player"
        val coins = prefs.getLong("coins", 0L)
        
        tvCurrentName.text = "Tên hiện tại: $currentName"
        tvCoins.text = "Xu: $coins"
        etPlayerName.setText(currentName)

        btnSaveName.setOnClickListener {
            val newName = etPlayerName.text.toString().trim()
            if (newName.isNotEmpty()) {
                prefs.edit().putString("player_name", newName).apply()
                tvCurrentName.text = "Tên hiện tại: $newName"
                Toast.makeText(this, "Đã lưu tên: $newName", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Vui lòng nhập tên", Toast.LENGTH_SHORT).show()
            }
        }

        btnBack.setOnClickListener {
            finish()
        }
    }
}
