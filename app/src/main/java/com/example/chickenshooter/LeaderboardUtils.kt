package com.example.chickenshooter

import android.content.Context

data class ScoreEntry(val score: Int, val timestamp: Long)
data class TimeEntry(val completionTimeMs: Long, val timestamp: Long)

object LeaderboardUtils {
    fun saveScore(context: Context, score: Int) {
        val prefs = context.getSharedPreferences("leaderboard", Context.MODE_PRIVATE)
        val oldScores = prefs.getString("scores", "") ?: ""
        val scores = oldScores.split(";")
            .filter { it.isNotEmpty() }
            .map {
                val parts = it.split(",")
                ScoreEntry(parts[0].toInt(), parts[1].toLong())
            }.toMutableList()
        scores.add(ScoreEntry(score, System.currentTimeMillis()))
        // Chỉ lấy 10 điểm cao nhất
        val topScores = scores.sortedByDescending { it.score }.take(10)
        val saveString = topScores.joinToString(";") { "${it.score},${it.timestamp}" }
        prefs.edit().putString("scores", saveString).apply()
    }

    fun getLeaderboard(context: Context): List<ScoreEntry> {
        val prefs = context.getSharedPreferences("leaderboard", Context.MODE_PRIVATE)
        val oldScores = prefs.getString("scores", "") ?: ""
        return oldScores.split(";")
            .filter { it.isNotEmpty() }
            .map {
                val parts = it.split(",")
                ScoreEntry(parts[0].toInt(), parts[1].toLong())
            }
    }
    
    fun saveCompletionTime(context: Context, completionTimeMs: Long) {
        val prefs = context.getSharedPreferences("leaderboard", Context.MODE_PRIVATE)
        val oldTimes = prefs.getString("completion_times", "") ?: ""
        val times = oldTimes.split(";")
            .filter { it.isNotEmpty() }
            .map {
                val parts = it.split(",")
                TimeEntry(parts[0].toLong(), parts[1].toLong())
            }.toMutableList()
        times.add(TimeEntry(completionTimeMs, System.currentTimeMillis()))
        // Chỉ lấy 10 thời gian nhanh nhất
        val topTimes = times.sortedBy { it.completionTimeMs }.take(10)
        val saveString = topTimes.joinToString(";") { "${it.completionTimeMs},${it.timestamp}" }
        prefs.edit().putString("completion_times", saveString).apply()
    }

    fun getCompletionTimes(context: Context): List<TimeEntry> {
        val prefs = context.getSharedPreferences("leaderboard", Context.MODE_PRIVATE)
        val oldTimes = prefs.getString("completion_times", "") ?: ""
        return oldTimes.split(";")
            .filter { it.isNotEmpty() }
            .map {
                val parts = it.split(",")
                TimeEntry(parts[0].toLong(), parts[1].toLong())
            }
    }
}