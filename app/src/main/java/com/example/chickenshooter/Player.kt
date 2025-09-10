package com.example.chickenshooter

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect

class Player(
    var x: Int,
    var y: Int,
    var bitmap: Bitmap,
    var lives: Int = 3,        // thêm số mạng
    var gold: Int = 0
) {
    var hasShield: Boolean = false
    private var shieldEndTime: Long = 0L

    // Animation nổ
    var explosionFrames: List<Bitmap> = emptyList()
    private var isExploding = false
    private var explosionFrameIndex = 0
    private var explosionStartTime = 0L

    // Trạng thái nhấp nháy sau khi nổ
    private var isBlinking = false
    private var blinkStartTime = 0L
    private val blinkDuration = 2000L // 2 giây
    private val blinkInterval = 200L

    // Paint khiên
    private val shieldStrokePaint = Paint().apply {
        color = Color.argb(128, 0, 200, 255)
        style = Paint.Style.STROKE
        strokeWidth = 10f
        isAntiAlias = true
    }
    private val shieldFillPaint = Paint().apply {
        color = Color.argb(40, 0, 200, 255)
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    fun getRect(): Rect =
        Rect(x, y, x + bitmap.width, y + bitmap.height)

    fun draw(canvas: Canvas) {
        when {
            isExploding -> {
                if (explosionFrames.isNotEmpty() && explosionFrameIndex < explosionFrames.size) {
                    val explosion = explosionFrames[explosionFrameIndex]
                    val drawX = x + bitmap.width / 2f - explosion.width / 2f
                    val drawY = y + bitmap.height / 2f - explosion.height / 2f
                    canvas.drawBitmap(explosion, drawX, drawY, null)
                }
            }


            isBlinking -> {
                val elapsed = System.currentTimeMillis() - blinkStartTime
                val visible = (elapsed / blinkInterval) % 2 == 0L
                if (visible) {
                    canvas.drawBitmap(bitmap, x.toFloat(), y.toFloat(), null)
                }
            }

            else -> {
                canvas.drawBitmap(bitmap, x.toFloat(), y.toFloat(), null)
            }
        }

        // Vẽ khiên
        if (hasShield) {
            val currentTime = System.currentTimeMillis()
            val remaining = shieldEndTime - currentTime
            val shouldDraw = if (remaining < 500) {
                (currentTime / 150) % 2 == 0L
            } else true

            if (shouldDraw) {
                val centerX = x + bitmap.width / 2f
                val centerY = y + bitmap.height / 2f
                val radius = (bitmap.width.coerceAtLeast(bitmap.height) * 0.7f)

                canvas.drawCircle(centerX, centerY, radius, shieldStrokePaint)
                canvas.drawCircle(centerX, centerY, radius - 4f, shieldFillPaint)
            }
        }
    }

    fun update() {
        val currentTime = System.currentTimeMillis()

        if (isExploding) {
            val frameDuration = 10L
            val elapsed = currentTime - explosionStartTime
            explosionFrameIndex = (elapsed / frameDuration).toInt()

            if (explosionFrameIndex >= explosionFrames.size) {
                isExploding = false
                isBlinking = true
                blinkStartTime = currentTime
            }
        }

        if (isBlinking && currentTime - blinkStartTime > blinkDuration) {
            isBlinking = false
        }

        if (hasShield && currentTime > shieldEndTime) {
            hasShield = false
        }
    }

    fun activateShield(durationMs: Long) {
        hasShield = true
        shieldEndTime = System.currentTimeMillis() + durationMs
    }

    // Gọi khi trúng đạn/gà
    fun hit(frames: List<Bitmap>) {
        if (hasShield || isExploding || isBlinking) return

        // Scale từng frame nổ về cùng kích thước player
        explosionFrames = frames.map { frame ->
            Bitmap.createScaledBitmap(frame, bitmap.width, bitmap.height, true)
        }
        isExploding = true
        explosionStartTime = System.currentTimeMillis()
        explosionFrameIndex = 0
    }

    fun isAlive(): Boolean = lives > 0
}

