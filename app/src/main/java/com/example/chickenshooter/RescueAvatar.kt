package com.example.chickenshooter

import android.graphics.*
import kotlin.math.*

/**
 * RescueAvatar - Quản lý hiệu ứng "cứu" nhân vật sau khi đánh bại boss
 * Avatar bay từ vị trí boss tới vị trí avatar dock với bezier curve và hiệu ứng
 */
class RescueAvatar(
    private val avatarBitmap: Bitmap,
    private val startX: Float,
    private val startY: Float,
    private val endX: Float,
    private val endY: Float,
    private val durationFrames: Int = 60 // Thời gian bay (frames)
) {
    private var currentFrame = 0
    private var currentX = startX
    private var currentY = startY
    private var currentScale = 1.0f
    private var currentRotation = 0f
    private var glowIntensity = 0f
    
    var arrived = false
        private set
    
    // Bezier curve control points
    private val controlX1 = startX + (endX - startX) * 0.25f + sin(PI/4).toFloat() * 200f
    private val controlY1 = startY + (endY - startY) * 0.25f - 150f
    private val controlX2 = startX + (endX - startX) * 0.75f - sin(PI/4).toFloat() * 100f
    private val controlY2 = startY + (endY - startY) * 0.75f - 100f
    
    // Paint objects cho hiệu ứng
    private val glowPaint = Paint().apply {
        isAntiAlias = true
        maskFilter = BlurMaskFilter(20f, BlurMaskFilter.Blur.NORMAL)
    }
    
    private val avatarPaint = Paint().apply {
        isAntiAlias = true
        isFilterBitmap = true
    }
    
    fun update() {
        if (arrived) return
        
        currentFrame++
        
        // Tính toán vị trí dựa trên bezier curve
        val t = currentFrame.toFloat() / durationFrames.toFloat()
        val clampedT = t.coerceIn(0f, 1f)
        
        // Cubic bezier curve calculation
        val oneMinusT = 1f - clampedT
        val oneMinusT2 = oneMinusT * oneMinusT
        val oneMinusT3 = oneMinusT2 * oneMinusT
        val t2 = clampedT * clampedT
        val t3 = t2 * clampedT
        
        currentX = oneMinusT3 * startX + 
                  3f * oneMinusT2 * clampedT * controlX1 + 
                  3f * oneMinusT * t2 * controlX2 + 
                  t3 * endX
                  
        currentY = oneMinusT3 * startY + 
                  3f * oneMinusT2 * clampedT * controlY1 + 
                  3f * oneMinusT * t2 * controlY2 + 
                  t3 * endY
        
        // Hiệu ứng scale (phình to rồi thu nhỏ)
        val scaleCurve = sin(clampedT * PI).toFloat()
        currentScale = 1f + scaleCurve * 0.3f // Scale từ 1.0 -> 1.3 -> 1.0
        
        // Hiệu ứng xoay nhẹ
        currentRotation = clampedT * 360f * 2f // Xoay 2 vòng trong quá trình bay
        
        // Hiệu ứng glow tăng dần khi gần đến đích
        glowIntensity = clampedT * 0.8f // Từ 0 -> 0.8
        
        // Kiểm tra đã đến đích chưa
        if (currentFrame >= durationFrames) {
            arrived = true
            currentX = endX
            currentY = endY
            currentScale = 1f
            currentRotation = 0f
            glowIntensity = 1f
        }
    }
    
    fun draw(canvas: Canvas) {
        val matrix = Matrix()
        
        // Tính toán kích thước avatar
        val avatarSize = 80f * currentScale
        val halfSize = avatarSize / 2f
        
        // Scale avatar bitmap
        val scaledAvatar = Bitmap.createScaledBitmap(
            avatarBitmap,
            avatarSize.toInt(),
            avatarSize.toInt(),
            true
        )
        
        // Vẽ hiệu ứng glow phía sau
        if (glowIntensity > 0f) {
            val glowRadius = avatarSize * (1f + glowIntensity * 0.5f)
            val glowAlpha = (glowIntensity * 255 * 0.6f).toInt().coerceIn(0, 255)
            
            // Vẽ nhiều lớp glow với màu khác nhau
            glowPaint.color = Color.argb(glowAlpha, 255, 215, 0) // Gold
            canvas.drawCircle(currentX, currentY, glowRadius * 0.8f, glowPaint)
            
            glowPaint.color = Color.argb(glowAlpha / 2, 255, 255, 255) // White
            canvas.drawCircle(currentX, currentY, glowRadius * 0.6f, glowPaint)
            
            glowPaint.color = Color.argb(glowAlpha / 3, 135, 206, 250) // Light blue
            canvas.drawCircle(currentX, currentY, glowRadius * 0.4f, glowPaint)
        }
        
        // Chuẩn bị matrix cho transformation
        matrix.reset()
        matrix.postScale(currentScale, currentScale)
        matrix.postRotate(currentRotation)
        matrix.postTranslate(currentX - halfSize, currentY - halfSize)
        
        // Vẽ avatar với circular clipping
        val savedLayer = canvas.saveLayer(
            currentX - halfSize, currentY - halfSize,
            currentX + halfSize, currentY + halfSize,
            null
        )
        
        // Tạo circular mask
        val circlePaint = Paint().apply {
            isAntiAlias = true
        }
        canvas.drawCircle(currentX, currentY, halfSize, circlePaint)
        
        // Áp dụng SRC_IN mode để clip
        circlePaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(scaledAvatar, matrix, circlePaint)
        
        // Restore layer
        canvas.restoreToCount(savedLayer)
        
        // Vẽ viền trắng cho avatar
        val borderPaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 3f * currentScale
            isAntiAlias = true
        }
        canvas.drawCircle(currentX, currentY, halfSize, borderPaint)
        
        // Vẽ hiệu ứng particle trail nếu đang bay
        if (!arrived) {
            drawParticleTrail(canvas)
        }
        
        // Vẽ hiệu ứng sparkle khi gần đến đích
        if (glowIntensity > 0.5f) {
            drawSparkleEffect(canvas)
        }
    }
    
    private fun drawParticleTrail(canvas: Canvas) {
        val particlePaint = Paint().apply {
            isAntiAlias = true
        }
        
        val numParticles = 8
        val trailLength = 50f
        
        for (i in 0 until numParticles) {
            val offset = i * (trailLength / numParticles)
            val alpha = (255 * (1f - i.toFloat() / numParticles) * 0.6f).toInt()
            
            // Tính vị trí particle dựa trên hướng di chuyển
            val angle = atan2(currentY - startY, currentX - startX) + PI.toFloat()
            val particleX = currentX + cos(angle) * offset
            val particleY = currentY + sin(angle) * offset
            
            particlePaint.color = Color.argb(alpha, 255, 215, 0) // Gold trail
            val particleSize = (3f + i * 0.5f) * currentScale
            canvas.drawCircle(particleX, particleY, particleSize, particlePaint)
        }
    }
    
    private fun drawSparkleEffect(canvas: Canvas) {
        val sparklePaint = Paint().apply {
            color = Color.WHITE
            strokeWidth = 2f
            isAntiAlias = true
        }
        
        val time = System.currentTimeMillis()
        val numSparkles = 6
        val sparkleRadius = 60f * currentScale
        
        for (i in 0 until numSparkles) {
            val angle = (i * 60f + time * 0.01f) % 360f
            val rad = Math.toRadians(angle.toDouble())
            val distance = sparkleRadius * (0.7f + 0.3f * sin(time * 0.005f + i).toFloat())
            
            val sparkleX = currentX + cos(rad).toFloat() * distance
            val sparkleY = currentY + sin(rad).toFloat() * distance
            
            // Vẽ sparkle như dấu sao 4 cánh
            val sparkleSize = 8f * currentScale
            canvas.drawLine(
                sparkleX - sparkleSize, sparkleY,
                sparkleX + sparkleSize, sparkleY,
                sparklePaint
            )
            canvas.drawLine(
                sparkleX, sparkleY - sparkleSize,
                sparkleX, sparkleY + sparkleSize,
                sparklePaint
            )
        }
    }
    
    fun getProgress(): Float {
        return if (durationFrames > 0) {
            (currentFrame.toFloat() / durationFrames.toFloat()).coerceIn(0f, 1f)
        } else 1f
    }
    
    fun getCurrentPosition(): PointF {
        return PointF(currentX, currentY)
    }
}
