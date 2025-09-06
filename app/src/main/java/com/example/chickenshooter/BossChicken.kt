package com.example.chickenshooter

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect

class BossChicken(
    var x: Int,
    var y: Int,
    val bitmap: Bitmap,
    var hp: Int,
    private val speed: Int,
    private val eggBitmap: Bitmap // bitmap cho trứng
) {
    // Thời gian giữa 2 lần bắn trứng (ms)
    private val eggShootInterval = 1500L
    private var lastEggShootTime: Long = 0

    fun update(currentTime: Long, eggs: MutableList<Egg>) {
        y += speed
        // Bắn trứng tỏa khi đến thời điểm
        if (currentTime - lastEggShootTime >= eggShootInterval) {
            lastEggShootTime = currentTime
            shootEggs(eggs)
        }
    }

    private fun shootEggs(eggs: MutableList<Egg>) {
        // Tỏa 5 hướng: lệch trái, chéo trái, thẳng, chéo phải, lệch phải
        val angles = listOf(60f, 70f, 80f , 90f, 100f, 110f, 120f)
        for (angle in angles) {
            eggs.add(
                Egg(
                    x + bitmap.width / 2 - eggBitmap.width / 2,
                    y + bitmap.height,
                    eggBitmap,
                    speed = 15f,
                    angleDegree = angle
                )
            )
        }
    }

    fun getRect(): Rect = Rect(x, y, x + bitmap.width, y + bitmap.height)
    fun draw(canvas: Canvas) {
        canvas.drawBitmap(bitmap, x.toFloat(), y.toFloat(), null)
    }
}