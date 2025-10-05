package com.example.chickenshooter

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button

class LeaderboardActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_leaderboard)

        val listView: ListView = findViewById(R.id.leaderboardListView)
        val leaderboard = LeaderboardUtils.getLeaderboard(this)
        val items = leaderboard.mapIndexed { i, entry ->
            "Top ${i+1}: ${entry.score} xu - ${android.text.format.DateFormat.format("dd/MM/yyyy HH:mm", entry.timestamp)}"
        }
        listView.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, items)

        // Thêm đoạn này ở đây:
        val btnClose = findViewById<Button>(R.id.btnClose)
        btnClose.setOnClickListener {
            finish()
        }
    }
}