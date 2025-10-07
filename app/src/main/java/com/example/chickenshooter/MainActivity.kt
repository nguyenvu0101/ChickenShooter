package com.example.chickenshooter

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Lấy id máy bay (player) người chơi đã chọn từ Intent
        val planeId = intent.getIntExtra("planeId", R.drawable.player)

        // Khởi tạo GameView với máy bay đã chọn (background sẽ tự động theo level)
        val gameView = GameView(this, planeId)
        setContentView(gameView)
    }
}