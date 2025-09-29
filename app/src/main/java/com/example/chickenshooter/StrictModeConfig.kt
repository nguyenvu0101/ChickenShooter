package com.example.chickenshooter

import android.os.StrictMode
import android.util.Log

/**
 * StrictMode configuration for detecting ANR-causing operations
 * Should be called in Application.onCreate() for debug builds
 */
object StrictModeConfig {
    private const val TAG = "StrictModeConfig"
    
    fun enable() {
        try {
            Log.d(TAG, "Enabling StrictMode for ANR detection...")
            
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()
                    .penaltyLog()
                    .penaltyFlashScreen()
                    .build()
            )
            
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    .build()
            )
            
            Log.d(TAG, "StrictMode enabled successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error enabling StrictMode: ${e.message}", e)
        }
    }
    
    fun disable() {
        try {
            Log.d(TAG, "Disabling StrictMode...")
            StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.LAX)
            StrictMode.setVmPolicy(StrictMode.VmPolicy.LAX)
            Log.d(TAG, "StrictMode disabled")
        } catch (e: Exception) {
            Log.e(TAG, "Error disabling StrictMode: ${e.message}", e)
        }
    }
}
