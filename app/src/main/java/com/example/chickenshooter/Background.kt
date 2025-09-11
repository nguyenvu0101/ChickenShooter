package com.example.chickenshooter

import android.graphics.Bitmap
import android.graphics.Canvas

class Background(
    private val bitmap: Bitmap,
    private val screenW: Int,
    private val screenH: Int,
    private val speed: Int = 8
) {
    private var bgY1 = 0
    private var bgY2 = -screenH
    private var scaledBitmap: Bitmap = Bitmap.createScaledBitmap(bitmap, screenW, screenH, true)

    fun update() {
        bgY1 += speed
        bgY2 += speed

        if (bgY1 >= screenH) {
            bgY1 = bgY2 - screenH
        }
        if (bgY2 >= screenH) {
            bgY2 = bgY1 - screenH
        }
    }

    fun draw(canvas: Canvas) {
        canvas.drawBitmap(scaledBitmap, 0f, bgY1.toFloat(), null)
        canvas.drawBitmap(scaledBitmap, 0f, bgY2.toFloat(), null)
    }
}
