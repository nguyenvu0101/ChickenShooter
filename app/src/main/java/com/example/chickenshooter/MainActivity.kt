package com.example.chickenshooter

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    // Danh sách máy bay, giống với ở StartMenuActivity
    private val planes = listOf(
        Plane("player", R.drawable.player_demo, R.drawable.player),
        Plane("player2_v2", R.drawable.player3_demo, R.drawable.player3),
        Plane("player_cart1", R.drawable.player_cart1_demo, R.drawable.player_cart1),
        Plane("player_cart2", R.drawable.player_cart2_demo, R.drawable.player_cart2)
        // ... thêm các máy bay khác nếu có
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Nhận id máy bay từ menu truyền sang
        val planeId = intent.getStringExtra("planeId") ?: "player"
        // Tìm máy bay trong danh sách, lấy ảnh góc thẳng (gameResId)
        val plane = planes.find { it.id == planeId } ?: planes[0]
        val planeGameResId = plane.gameResId

        // Khởi tạo GameView với máy bay đã chọn
        val gameView = GameView(this, planeGameResId)
        setContentView(gameView)
    }
}