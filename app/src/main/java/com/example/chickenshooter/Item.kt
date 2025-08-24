package com.example.chickenshooter

import android.graphics.Bitmap

class Item(
    x: Int,
    y: Int,
    bitmap: Bitmap,
    val type: Int // 0: bắn nhanh, 1: 3 viên song song, 2: 3 viên tỏa
) : GameObject(x, y, bitmap) {
    private val speed = 8
    override fun update() {
        y += speed
    }
}