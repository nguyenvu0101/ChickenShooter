package com.example.chickenshooter

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import kotlin.math.cos
import kotlin.math.sin

class Bullet(
    var x: Int,
    var y: Int,
    val bitmap: Bitmap,
    val speed: Int,
    val damage: Int,
    val angle: Double // độ (degree)
) {
    fun update() {
        val rad = Math.toRadians(angle)
        x += (speed * cos(rad)).toInt()
        y += (speed * -sin(rad)).toInt()
    }

    fun getRect(): Rect = Rect(x, y, x + bitmap.width, y + bitmap.height)

    fun draw(canvas: Canvas) {
        canvas.drawBitmap(bitmap, x.toFloat(), y.toFloat(), null)
    }
}