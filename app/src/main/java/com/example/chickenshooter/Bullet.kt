package com.example.chickenshooter

import android.graphics.Bitmap

class Bullet(x: Int, y: Int, bitmap: Bitmap, private val speed: Int, val damage: Int = 1, private val angle: Double = 270.0) : GameObject(x, y, bitmap) {
    // angle tính bằng độ, 270 là hướng lên trên
    override fun update() {
        val rad = Math.toRadians(angle)
        x += (speed * Math.cos(rad)).toInt()
        y += (speed * Math.sin(rad)).toInt()
    }
}