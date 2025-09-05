package com.example.chickenshooter

import android.graphics.Bitmap
import android.graphics.Canvas
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI

class Egg(
    x: Int,
    y: Int,
    bitmap: Bitmap,
    private val speed: Float = 15f,
    private val angleDegree: Float = 90f // 90 độ là rơi thẳng xuống
) : GameObject(x, y, bitmap) {
    var isOutOfScreen = false

    // Chuyển đổi sang radian để dùng sin/cos
    private val angleRad = angleDegree * (PI / 180f)
    private val dx = (cos(angleRad) * speed).toFloat()
    private val dy = (sin(angleRad) * speed).toFloat()

    override fun update() {
        x += dx.toInt()
        y += dy.toInt()
        // Nếu ra ngoài màn hình
        if (y > 1920 || x < -bitmap.width || x > 1080 + bitmap.width) {
            isOutOfScreen = true
        }
    }
}