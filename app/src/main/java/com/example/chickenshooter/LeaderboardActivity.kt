package com.example.chickenshooter

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.TextView

class LeaderboardActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_leaderboard)

        val listView: ListView = findViewById(R.id.leaderboardListView)
        
        // Hiển thị bảng xếp hạng thời gian hoàn thành
        val completionTimes = LeaderboardUtils.getCompletionTimes(this)
        val items = if (completionTimes.isNotEmpty()) {
            completionTimes.mapIndexed { i, entry ->
                val timeSeconds = entry.completionTimeMs / 1000.0
                val minutes = (timeSeconds / 60).toInt()
                val seconds = (timeSeconds % 60).toInt()
                "🏆 Top ${i+1}: ${minutes}:${String.format("%02d", seconds)} - ${android.text.format.DateFormat.format("dd/MM/yyyy HH:mm", entry.timestamp)}"
            }
        } else {
            listOf("📊 Chưa có dữ liệu thời gian hoàn thành")
        }
        
        // Tạo custom adapter với màu sắc phù hợp
        val adapter = object : ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, items) {
            override fun getView(position: Int, convertView: android.view.View?, parent: android.view.ViewGroup): android.view.View {
                val view = super.getView(position, convertView, parent)
                val textView = view.findViewById<TextView>(android.R.id.text1)
                textView.setTextColor(android.graphics.Color.WHITE)
                textView.textSize = 16f
                textView.setShadowLayer(2f, 1f, 1f, android.graphics.Color.BLACK)
                return view
            }
        }
        listView.adapter = adapter

        // Thêm đoạn này ở đây:
        val btnClose = findViewById<Button>(R.id.btnClose)
        btnClose.setOnClickListener {
            finish()
        }
    }
}