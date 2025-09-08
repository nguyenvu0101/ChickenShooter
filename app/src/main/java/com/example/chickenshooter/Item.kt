package com.example.chickenshooter

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect

enum class ItemType {
    FAST, PARALLEL, SPREAD, MANA
}

class Item(
    var x: Int,
    var y: Int,
    val bitmap: Bitmap,
    val type: ItemType , // Sử dụng enum thay vì Int
    val speed: Int = 10
) {

    fun update() {
        y += speed
    }

    fun getRect(): Rect = Rect(x, y, x + bitmap.width, y + bitmap.height)

    fun draw(canvas: Canvas) {
        canvas.drawBitmap(bitmap, x.toFloat(), y.toFloat(), null)
    }
}