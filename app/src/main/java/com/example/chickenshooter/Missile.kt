package com.example.chickenshooter

import android.graphics.Bitmap
import android.graphics.Canvas

class Missile(
    var x: Int,
    var y: Int,
    val bitmap: Bitmap,
    val targetX: Int,
    val targetY: Int,
    val speed: Int = 18
) {
    var isExploding = false
    var explosionFrame = 0
    val explosionMaxFrame = 18

    fun update() {
        if (!isExploding) {
            val dx = targetX - x
            val dy = targetY - y
            if (Math.abs(dx) > speed) x += if (dx > 0) speed else -speed else x = targetX
            if (Math.abs(dy) > speed) y += if (dy > 0) speed else -speed else y = targetY
            if (x == targetX && y == targetY) {
                isExploding = true
                explosionFrame = 0
            }
        } else {
            if (explosionFrame < explosionMaxFrame) explosionFrame++
        }
    }

    fun draw(canvas: Canvas, explosionBitmap: Bitmap) {
        if (!isExploding) {
            canvas.drawBitmap(bitmap, x.toFloat(), y.toFloat(), null)
        } else if (explosionFrame < explosionMaxFrame) {
            val minScale = 0.7f
            val maxScale = 2.5f
            val scale = minScale + (maxScale - minScale) * (explosionFrame.toFloat() / explosionMaxFrame)
            val explosionW = (explosionBitmap.width * scale).toInt()
            val explosionH = (explosionBitmap.height * scale).toInt()
            val drawX = x + bitmap.width / 2 - explosionW / 2
            val drawY = y + bitmap.height / 2 - explosionH / 2
            val scaledExplosion = Bitmap.createScaledBitmap(explosionBitmap, explosionW, explosionH, true)
            canvas.drawBitmap(scaledExplosion, drawX.toFloat(), drawY.toFloat(), null)
        }
    }

    fun isFinished(): Boolean = isExploding && explosionFrame >= explosionMaxFrame
}