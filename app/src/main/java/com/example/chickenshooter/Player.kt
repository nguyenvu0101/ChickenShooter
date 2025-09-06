package com.example.chickenshooter

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect

class Player(
    var x: Int,
    var y: Int,
    var bitmap: Bitmap ,
    var gold : Int = 0
) {
    fun getRect(): android.graphics.Rect =
        android.graphics.Rect(x, y, x + bitmap.width, y + bitmap.height)
    fun draw(canvas: Canvas) {
        canvas.drawBitmap(bitmap, x.toFloat(), y.toFloat(), null)
    }
}