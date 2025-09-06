package com.example.chickenshooter

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class ModeSelectActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mode_select)

        findViewById<Button>(R.id.btnOnline).setOnClickListener {
            // Quan trọng: đảm bảo không giữ cờ offline cũ
            getSharedPreferences("game", MODE_PRIVATE)
                .edit().putBoolean("offline_mode", false).apply()
            startActivity(Intent(this, LoginActivity::class.java))
        }

        findViewById<Button>(R.id.btnOffline).setOnClickListener {
            getSharedPreferences("game", MODE_PRIVATE)
                .edit().putBoolean("offline_mode", true).apply()
            startActivity(Intent(this, StartMenuActivity::class.java))
            finish()
        }
    }
}
