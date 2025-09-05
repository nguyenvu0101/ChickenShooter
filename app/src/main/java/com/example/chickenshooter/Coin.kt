package com.example.chickenshooter
import android.graphics.Bitmap
import android.graphics.Canvas

class Coin(var x: Int, var y: Int, val bitmap: Bitmap) {
    var isCollected = false

    fun update() {
        y += 10 // tốc độ rơi
    }

    fun draw(canvas: Canvas) {
        if (!isCollected) {
            canvas.drawBitmap(bitmap, x.toFloat(), y.toFloat(), null)
        }
    }

    fun getRect(): android.graphics.Rect {
        return android.graphics.Rect(x, y, x + bitmap.width, y + bitmap.height)
    }
}