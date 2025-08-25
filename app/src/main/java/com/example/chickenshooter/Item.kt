package com.example.chickenshooter

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect

class Item(
    var x: Int,
    var y: Int,
    val bitmap: Bitmap,
    val type: Int // 0: fast, 1: parallel, 2: spread
) {
    val speed = 10

    fun update() {
        y += speed
    }

    fun getRect(): Rect = Rect(x, y, x + bitmap.width, y + bitmap.height)

    fun draw(canvas: Canvas) {
        canvas.drawBitmap(bitmap, x.toFloat(), y.toFloat(), null)
    }
}