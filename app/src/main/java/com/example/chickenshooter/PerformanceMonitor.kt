package com.example.chickenshooter

import android.util.Log
import java.util.concurrent.atomic.AtomicLong

/**
 * Performance monitoring for input latency and frame timing
 * Tracks input-to-processing latency and frame performance
 */
class PerformanceMonitor {
    private val inputTimestamps = mutableListOf<Long>()
    private val processingTimestamps = mutableListOf<Long>()
    private val frameTimestamps = mutableListOf<Long>()
    
    private val maxSamples = 60 // Keep last 60 samples
    private val inputLatencyThreshold = 16L // 16ms threshold for 60fps
    
    private var lastFrameTime = 0L
    private var frameCount = 0L
    private var fpsStartTime = 0L
    
    companion object {
        private const val TAG = "PerformanceMonitor"
    }
    
    /**
     * Record input event timestamp
     */
    fun recordInput() {
        val timestamp = System.currentTimeMillis()
        inputTimestamps.add(timestamp)
        
        // Keep only recent samples
        if (inputTimestamps.size > maxSamples) {
            inputTimestamps.removeAt(0)
        }
        
        Log.d(TAG, "Input recorded at $timestamp")
    }
    
    /**
     * Record input processing completion
     */
    fun recordInputProcessed() {
        val timestamp = System.currentTimeMillis()
        processingTimestamps.add(timestamp)
        
        // Keep only recent samples
        if (processingTimestamps.size > maxSamples) {
            processingTimestamps.removeAt(0)
        }
        
        // Calculate latency if we have matching input
        if (inputTimestamps.isNotEmpty()) {
            val inputTime = inputTimestamps.lastOrNull()
            if (inputTime != null) {
                val latency = timestamp - inputTime
                if (latency > inputLatencyThreshold) {
                    Log.w(TAG, "High input latency: ${latency}ms (threshold: ${inputLatencyThreshold}ms)")
                } else {
                    Log.d(TAG, "Input latency: ${latency}ms")
                }
            }
        }
    }
    
    /**
     * Record frame timing
     */
    fun recordFrame() {
        val currentTime = System.currentTimeMillis()
        frameTimestamps.add(currentTime)
        
        // Keep only recent samples
        if (frameTimestamps.size > maxSamples) {
            frameTimestamps.removeAt(0)
        }
        
        // Calculate FPS every 60 frames
        frameCount++
        if (frameCount % 60 == 0L) {
            if (fpsStartTime == 0L) {
                fpsStartTime = currentTime
            } else {
                val elapsed = currentTime - fpsStartTime
                val fps = (frameCount * 1000) / elapsed
                Log.d(TAG, "FPS: $fps (frames: $frameCount, elapsed: ${elapsed}ms)")
                
                // Reset for next measurement
                frameCount = 0
                fpsStartTime = currentTime
            }
        }
        
        lastFrameTime = currentTime
    }
    
    /**
     * Get average input latency
     */
    fun getAverageInputLatency(): Long {
        if (inputTimestamps.size < 2 || processingTimestamps.size < 2) return 0
        
        val minSize = minOf(inputTimestamps.size, processingTimestamps.size)
        var totalLatency = 0L
        
        for (i in 0 until minSize) {
            val inputTime = inputTimestamps[i]
            val processTime = processingTimestamps[i]
            totalLatency += (processTime - inputTime)
        }
        
        return totalLatency / minSize
    }
    
    /**
     * Get current FPS
     */
    fun getCurrentFPS(): Float {
        if (frameTimestamps.size < 2) return 0f
        
        val recentFrames = frameTimestamps.takeLast(10) // Last 10 frames
        if (recentFrames.size < 2) return 0f
        
        val timeSpan = recentFrames.last() - recentFrames.first()
        val frameSpan = recentFrames.size - 1
        
        return if (timeSpan > 0) {
            (frameSpan * 1000f) / timeSpan
        } else {
            0f
        }
    }
    
    /**
     * Check if performance is within acceptable limits
     */
    fun isPerformanceAcceptable(): Boolean {
        val avgLatency = getAverageInputLatency()
        val fps = getCurrentFPS()
        
        val latencyOk = avgLatency <= inputLatencyThreshold
        val fpsOk = fps >= 50f // Minimum 50 FPS
        
        Log.d(TAG, "Performance check: latency=${avgLatency}ms (ok=$latencyOk), fps=${fps} (ok=$fpsOk)")
        
        return latencyOk && fpsOk
    }
    
    /**
     * Reset all measurements
     */
    fun reset() {
        inputTimestamps.clear()
        processingTimestamps.clear()
        frameTimestamps.clear()
        frameCount = 0
        fpsStartTime = 0
        lastFrameTime = 0
        Log.d(TAG, "Performance monitor reset")
    }
}
