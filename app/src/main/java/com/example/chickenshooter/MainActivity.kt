package com.example.chickenshooter

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Lấy id máy bay (ảnh góc thẳng) người chơi đã chọn từ Intent
        val planeGameResId = intent.getIntExtra("planeGameResId", R.drawable.player)

        // Khởi tạo GameView với máy bay đã chọn
        val gameView = GameView(this, planeGameResId)
        setContentView(gameView)
    }
}