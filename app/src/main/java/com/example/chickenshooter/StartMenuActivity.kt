package com.example.chickenshooter

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class StartMenuActivity : AppCompatActivity() {
    private var selectedBackground = R.drawable.background
    private var selectedPlane = R.drawable.player

    private lateinit var selectedBgView: ImageView
    private lateinit var selectedPlaneView: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start_menu)

        val bg1 = findViewById<ImageView>(R.id.bg1Img)
        val bg2 = findViewById<ImageView>(R.id.bg2Img)
        val plane1 = findViewById<ImageView>(R.id.plane1Img)
        val plane2 = findViewById<ImageView>(R.id.plane2Img)

        selectedBgView = bg1
        selectedPlaneView = plane1

        // Hiệu ứng chọn ban đầu
        selectedBgView.setBackgroundResource(R.drawable.bg_selected_border)
        selectedPlaneView.setBackgroundResource(R.drawable.bg_selected_border)

        // Chọn background
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

        // Chọn máy bay
        plane1.setOnClickListener {
            selectedPlane = R.drawable.player
            selectedPlaneView.background = null
            plane1.setBackgroundResource(R.drawable.bg_selected_border)
            selectedPlaneView = plane1
        }
        plane2.setOnClickListener {
            selectedPlane = R.drawable.player2
            selectedPlaneView.background = null
            plane2.setBackgroundResource(R.drawable.bg_selected_border)
            selectedPlaneView = plane2
        }

        // Bắt đầu game
        findViewById<Button>(R.id.startBtn).setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("backgroundId", selectedBackground)
            intent.putExtra("planeId", selectedPlane)
            startActivity(intent)
        }
    }
}