package com.example.chickenshooter

import android.graphics.Bitmap

class Player(x: Int, y: Int, bitmap: Bitmap) : GameObject(x, y, bitmap) {
    fun move(dx: Int, dy: Int, screenWidth: Int, screenHeight: Int) {
        x = (x + dx).coerceIn(0, screenWidth - bitmap.width)
        y = (y + dy).coerceIn(0, screenHeight - bitmap.height)
    }
}