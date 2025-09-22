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
    private val angleDegree: Float = 90f, // 90 độ là rơi thẳng xuống
    private val screenHeight: Int,
    private val screenWidth: Int
) : GameObject(x, y, bitmap) {

    // Chuyển đổi sang radian để dùng sin/cos
    private val angleRad = angleDegree * (PI / 180f)
    private val dx = (cos(angleRad) * speed).toFloat()
    private val dy = (sin(angleRad) * speed).toFloat()

    override fun update() {
        x += dx.toInt()
        y += dy.toInt()
    }

    val isOutOfScreen: Boolean
        get() = y > screenHeight || x < -bitmap.width || x > screenWidth + bitmap.width
}