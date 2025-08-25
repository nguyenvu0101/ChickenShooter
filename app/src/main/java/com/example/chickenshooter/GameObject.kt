package com.example.chickenshooter

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect

open class GameObject(var x: Int, var y: Int, var bitmap: Bitmap) {
    open fun update() {}
    open fun draw(canvas: Canvas) {
        canvas.drawBitmap(bitmap, x.toFloat(), y.toFloat(), null)
    }
    fun getRect(): Rect = Rect(x, y, x + bitmap.width, y + bitmap.height)
}