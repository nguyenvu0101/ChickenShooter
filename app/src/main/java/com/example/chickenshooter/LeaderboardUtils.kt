package com.example.chickenshooter

import android.content.Context

data class ScoreEntry(
    val score: Int,
    val timestamp: Long,
    val completionTime: Long // Thời gian hoàn thành game (tính bằng mili giây)
)

object LeaderboardUtils {
    fun saveScore(context: Context, score: Int, completionTime: Long) {
        val prefs = context.getSharedPreferences("leaderboard", Context.MODE_PRIVATE)
        val oldScores = prefs.getString("scores", "") ?: ""
        val scores = oldScores.split(";")
            .filter { it.isNotEmpty() }
            .map {
                val parts = it.split(",")
                ScoreEntry(
                    parts[0].toInt(),
                    parts[1].toLong(),
                    if(parts.size > 2) parts[2].toLong() else Long.MAX_VALUE // Nếu cũ thì gán giá trị lớn
                )
            }.toMutableList()
        scores.add(ScoreEntry(score, System.currentTimeMillis(), completionTime))
        // Chỉ lấy 10 thành tích nhanh nhất
        val topScores = scores.sortedBy { it.completionTime }.take(10)
        val saveString = topScores.joinToString(";") { "${it.score},${it.timestamp},${it.completionTime}" }
        prefs.edit().putString("scores", saveString).apply()
    }

    fun getLeaderboard(context: Context): List<ScoreEntry> {
        val prefs = context.getSharedPreferences("leaderboard", Context.MODE_PRIVATE)
        val oldScores = prefs.getString("scores", "") ?: ""
        return oldScores.split(";")
            .filter { it.isNotEmpty() }
            .map {
                val parts = it.split(",")
                ScoreEntry(
                    parts[0].toInt(),
                    parts[1].toLong(),
                    if(parts.size > 2) parts[2].toLong() else Long.MAX_VALUE
                )
            }
    }
}