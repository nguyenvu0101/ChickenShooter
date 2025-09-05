package com.example.chickenshooter

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseApp

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        // Lấy id background và id máy bay (player) người chơi đã chọn từ Intent
        val backgroundId = intent.getIntExtra("backgroundId", R.drawable.background)
        val planeId = intent.getIntExtra("planeId", R.drawable.player)

        // Khởi tạo GameView với background và máy bay đã chọn
        val gameView = GameView(this, backgroundId, planeId)
        setContentView(gameView)
    }
}