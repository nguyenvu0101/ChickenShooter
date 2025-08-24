package com.example.chickenshooter

import android.graphics.Bitmap

class Chicken(
    x: Int,
    y: Int,
    bitmap: Bitmap,
    private val speed: Int,
    private val moveType: Int,
    var hp: Int = 3
) : GameObject(x, y, bitmap) {
    private var tick = 0
    override fun update() {
        tick++
        when (moveType) {
            0 -> y += speed // đi thẳng xuống
            1 -> { // zigzag
                y += speed
                x += (Math.sin(tick / 10.0) * 10).toInt()
            }
            2 -> { // lượn sóng
                y += speed
                x += (Math.sin(tick / 5.0) * 20).toInt()
            }
            3 -> { // kiểu đặc biệt (cosine)
                y += speed
                x += (Math.cos(tick / 8.0) * 15).toInt()
            }
            else -> { // dự phòng cho các moveType khác
                y += speed
            }
        }
    }
}