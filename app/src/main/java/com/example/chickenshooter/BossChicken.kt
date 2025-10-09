package com.example.chickenshooter

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect

class BossChicken(
    var x: Int,
    var y: Int,
    val bitmap: Bitmap,
    var hp: Int,
    private var vx: Int, // tốc độ ngang
    private var vy: Int, // tốc độ dọc
    private val eggBitmap: Bitmap,
    private val screenWidth: Int,
    private val screenHeight: Int,
    private val eggCount: Int = 3 // số lượng đạn bắn
) {
    private val eggShootInterval = 3000L
    private var lastEggShootTime: Long = 0
    val maxHp: Int = hp

    // chỉ có 1 hàm update!
    fun update(currentTime: Long, eggs: MutableList<Egg>) {
        // Di chuyển boss trong nửa trên màn hình, đổi hướng khi chạm biên
        x += vx
        y += vy

        val minX = 0
        val maxX = screenWidth - bitmap.width
        val minY = 0
        val maxY = screenHeight / 2 - bitmap.height

        if (x < minX) {
            x = minX
            vx = -vx
        }
        if (x > maxX) {
            x = maxX
            vx = -vx
        }
        if (y < minY) {
            y = minY
            vy = -vy
        }
        if (y > maxY) {
            y = maxY
            vy = -vy
        }

        // Bắn trứng như cũ
        if (currentTime - lastEggShootTime >= eggShootInterval) {
            lastEggShootTime = currentTime
            shootEggs(eggs)
        }
    }

    private fun shootEggs(eggs: MutableList<Egg>) {
        // Tính toán góc dựa trên số lượng đạn
        val startAngle = 80f
        val endAngle = 120f
        val angleStep = if (eggCount > 1) (endAngle - startAngle) / (eggCount - 1) else 0f
        
        for (i in 0 until eggCount) {
            val angle = startAngle + (i * angleStep)
            eggs.add(
                Egg(
                    x + bitmap.width / 2 - eggBitmap.width / 2,
                    y + bitmap.height,
                    eggBitmap,
                    speed = 7.5f,
                    angleDegree = angle,
                    screenHeight = this.screenHeight,
                    screenWidth = this.screenWidth
                )
            )
        }
    }

    fun getRect(): Rect = Rect(x, y, x + bitmap.width, y + bitmap.height)
    fun draw(canvas: Canvas) {
        canvas.drawBitmap(bitmap, x.toFloat(), y.toFloat(), null)
    }
}