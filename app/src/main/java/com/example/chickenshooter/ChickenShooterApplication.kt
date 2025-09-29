package com.example.chickenshooter

import android.app.Application
import android.util.Log

/**
 * Application class for ChickenShooter game
 * Enables StrictMode for ANR detection in debug builds
 */
class ChickenShooterApplication : Application() {
    
    companion object {
        private const val TAG = "ChickenShooterApp"
    }
    
    override fun onCreate() {
        super.onCreate()
        
        try {
            Log.d(TAG, "ChickenShooterApplication onCreate()")
            
            // Enable StrictMode for ANR detection in debug builds
            // Use ApplicationInfo flags to detect debug build instead of BuildConfig
            val isDebug = try {
                (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
            } catch (e: Exception) {
                Log.w(TAG, "Could not determine debug status, defaulting to debug mode")
                true // Default to debug mode for safety
            }
            
            if (isDebug) {
                StrictModeConfig.enable()
                Log.d(TAG, "StrictMode enabled for debug build")
            } else {
                Log.d(TAG, "StrictMode disabled for release build")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in Application.onCreate(): ${e.message}", e)
        }
    }
    
    override fun onTerminate() {
        super.onTerminate()
        try {
            Log.d(TAG, "ChickenShooterApplication onTerminate()")
            // Use ApplicationInfo flags to detect debug build instead of BuildConfig
            val isDebug = try {
                (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
            } catch (e: Exception) {
                Log.w(TAG, "Could not determine debug status, defaulting to debug mode")
                true // Default to debug mode for safety
            }
            
            if (isDebug) {
                StrictModeConfig.disable()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in Application.onTerminate(): ${e.message}", e)
        }
    }
}
