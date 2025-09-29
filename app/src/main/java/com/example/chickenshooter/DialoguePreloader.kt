package com.example.chickenshooter

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.graphics.Paint
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.RectF
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log

/**
 * Preloaded resources for dialogue scenes
 * All assets are loaded off-main-thread and cached for immediate use
 */
data class DialogueSceneResources(
    val avatarLeft: Bitmap,
    val avatarRight: Bitmap,
    val bubbleFrame: Bitmap,
    val background: Bitmap,
    val font: Typeface,
    val clickSfxId: Int,
    val advanceSfxId: Int,
    val paint: Paint,
    val textPaint: Paint,
    val speakerNamePaint: Paint,
    val hintPaint: Paint,
    val borderPaint: Paint
)

/**
 * DialoguePreloader - Loads and caches all dialogue assets off-main-thread
 * Uses coroutines and proper bitmap scaling to prevent ANR
 */
class DialoguePreloader(private val context: Context) {
    
    companion object {
        private const val TAG = "DialoguePreloader"
        private const val AVATAR_SIZE = 100
        private const val BUBBLE_WIDTH = 400
        private const val BUBBLE_HEIGHT = 200
    }
    
    /**
     * Preload all assets for a dialogue scene off-main-thread
     */
    suspend fun preload(scene: DialogueScene, avatarMap: Map<Speaker, Bitmap>): DialogueSceneResources {
        return withContext(Dispatchers.IO) {
            Log.d(TAG, "Starting asset preload for scene with ${scene.lines.size} lines")
            
            try {
                // Preload avatars with proper scaling
                val avatarLeft = preloadAvatar(avatarMap[Speaker.JACK] ?: createFallbackAvatar())
                val avatarRight = preloadAvatar(avatarMap[Speaker.BOSS] ?: createFallbackAvatar())
                
                // Preload UI elements
                val bubbleFrame = preloadBubbleFrame()
                val background = preloadBackground()
                
                // Preload fonts and paints
                val font = preloadFont()
                val paint = preloadPaint()
                val textPaint = preloadTextPaint(font)
                val speakerNamePaint = preloadSpeakerNamePaint(font)
                val hintPaint = preloadHintPaint(font)
                val borderPaint = preloadBorderPaint()
                
                // Preload audio (return dummy IDs for now)
                val clickSfxId = 0 // Will be loaded by SoundPool
                val advanceSfxId = 1
                
                Log.d(TAG, "Asset preload completed successfully")
                
                DialogueSceneResources(
                    avatarLeft = avatarLeft,
                    avatarRight = avatarRight,
                    bubbleFrame = bubbleFrame,
                    background = background,
                    font = font,
                    clickSfxId = clickSfxId,
                    advanceSfxId = advanceSfxId,
                    paint = paint,
                    textPaint = textPaint,
                    speakerNamePaint = speakerNamePaint,
                    hintPaint = hintPaint,
                    borderPaint = borderPaint
                )
                
            } catch (e: Exception) {
                Log.e(TAG, "Error during asset preload: ${e.message}", e)
                throw e
            }
        }
    }
    
    private fun preloadAvatar(originalBitmap: Bitmap): Bitmap {
        return try {
            // Scale avatar to target size with proper config
            val options = BitmapFactory.Options().apply {
                inSampleSize = calculateSampleSize(originalBitmap.width, AVATAR_SIZE)
                inPreferredConfig = Bitmap.Config.RGB_565 // No alpha needed for avatars
            }
            
            val scaledBitmap = Bitmap.createScaledBitmap(
                originalBitmap,
                AVATAR_SIZE,
                AVATAR_SIZE,
                true
            )
            
            Log.d(TAG, "Avatar preloaded: ${scaledBitmap.width}x${scaledBitmap.height}")
            scaledBitmap
        } catch (e: Exception) {
            Log.e(TAG, "Error preloading avatar: ${e.message}", e)
            createFallbackAvatar()
        }
    }
    
    private fun preloadBubbleFrame(): Bitmap {
        return try {
            // Create a simple speech bubble frame
            val bitmap = Bitmap.createBitmap(BUBBLE_WIDTH, BUBBLE_HEIGHT, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            
            val paint = Paint().apply {
                color = android.graphics.Color.argb(240, 25, 25, 50)
                isAntiAlias = true
            }
            
            val rect = RectF(0f, 0f, BUBBLE_WIDTH.toFloat(), BUBBLE_HEIGHT.toFloat())
            canvas.drawRoundRect(rect, 25f, 25f, paint)
            
            Log.d(TAG, "Bubble frame preloaded: ${bitmap.width}x${bitmap.height}")
            bitmap
        } catch (e: Exception) {
            Log.e(TAG, "Error preloading bubble frame: ${e.message}", e)
            createFallbackBubble()
        }
    }
    
    private fun preloadBackground(): Bitmap {
        return try {
            // Create a simple dark overlay
            val bitmap = Bitmap.createBitmap(800, 600, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            
            val paint = Paint().apply {
                color = android.graphics.Color.argb(180, 0, 0, 0)
            }
            
            canvas.drawRect(0f, 0f, 800f, 600f, paint)
            
            Log.d(TAG, "Background preloaded: ${bitmap.width}x${bitmap.height}")
            bitmap
        } catch (e: Exception) {
            Log.e(TAG, "Error preloading background: ${e.message}", e)
            createFallbackBackground()
        }
    }
    
    private fun preloadFont(): Typeface {
        return try {
            Typeface.DEFAULT_BOLD
        } catch (e: Exception) {
            Log.e(TAG, "Error preloading font: ${e.message}", e)
            Typeface.DEFAULT
        }
    }
    
    private fun preloadPaint(): Paint {
        return Paint().apply {
            color = android.graphics.Color.argb(240, 25, 25, 50)
            isAntiAlias = true
        }
    }
    
    private fun preloadTextPaint(font: Typeface): Paint {
        return Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 32f
            isAntiAlias = true
            typeface = font
        }
    }
    
    private fun preloadSpeakerNamePaint(font: Typeface): Paint {
        return Paint().apply {
            color = android.graphics.Color.YELLOW
            textSize = 36f
            isAntiAlias = true
            typeface = font
        }
    }
    
    private fun preloadHintPaint(font: Typeface): Paint {
        return Paint().apply {
            color = android.graphics.Color.argb(150, 255, 255, 255)
            textSize = 24f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = font
        }
    }
    
    private fun preloadBorderPaint(): Paint {
        return Paint().apply {
            color = android.graphics.Color.argb(100, 255, 255, 255)
            style = Paint.Style.STROKE
            strokeWidth = 2f
            isAntiAlias = true
        }
    }
    
    private fun createFallbackAvatar(): Bitmap {
        val bitmap = Bitmap.createBitmap(AVATAR_SIZE, AVATAR_SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            color = android.graphics.Color.GRAY
        }
        canvas.drawCircle(AVATAR_SIZE / 2f, AVATAR_SIZE / 2f, AVATAR_SIZE / 2f, paint)
        return bitmap
    }
    
    private fun createFallbackBubble(): Bitmap {
        val bitmap = Bitmap.createBitmap(BUBBLE_WIDTH, BUBBLE_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            color = android.graphics.Color.argb(240, 25, 25, 50)
        }
        val rect = RectF(0f, 0f, BUBBLE_WIDTH.toFloat(), BUBBLE_HEIGHT.toFloat())
        canvas.drawRoundRect(rect, 25f, 25f, paint)
        return bitmap
    }
    
    private fun createFallbackBackground(): Bitmap {
        val bitmap = Bitmap.createBitmap(800, 600, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            color = android.graphics.Color.argb(180, 0, 0, 0)
        }
        canvas.drawRect(0f, 0f, 800f, 600f, paint)
        return bitmap
    }
    
    private fun calculateSampleSize(originalSize: Int, targetSize: Int): Int {
        var sampleSize = 1
        while (originalSize / sampleSize > targetSize) {
            sampleSize *= 2
        }
        return sampleSize
    }
}
