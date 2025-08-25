package com.example.chickenshooter

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect

class BossChicken(
    var x: Int,
    var y: Int,
    val bitmap: Bitmap,
    var hp: Int,
    private val speed: Int
) {
    fun update() {
        y += speed
    }

    fun getRect(): Rect {
        return Rect(x, y, x + bitmap.width, y + bitmap.height)
    }

    fun draw(canvas: Canvas) {
        canvas.drawBitmap(bitmap, x.toFloat(), y.toFloat(), null)
    }
    fun getBitmap(): Bitmap {
        return bitmap
    }
}