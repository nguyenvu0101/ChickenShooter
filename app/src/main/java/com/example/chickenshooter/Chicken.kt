package com.example.chickenshooter

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect

class Chicken(
    var x: Int,
    var y: Int,
    val bitmap: Bitmap,
    val speed: Int,
    val moveType: Int,
    var hp: Int
) {
    private var tick = 0

    fun update() {
        tick++
        when (moveType) {
            0 -> { y += speed }
            1 -> { y += speed; x += (Math.sin(tick / 10.0) * 10).toInt() }
            2 -> { y += speed; x += (Math.cos(tick / 8.0) * 14).toInt() }
            3 -> { y += speed + (tick % 3) }
            else -> { y += speed }
        }
    }

    fun getRect(): Rect = Rect(x, y, x + bitmap.width, y + bitmap.height)

    fun draw(canvas: Canvas) {
        canvas.drawBitmap(bitmap, x.toFloat(), y.toFloat(), null)
    }
}