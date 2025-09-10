package com.example.chickenshooter

import android.graphics.Bitmap
import android.graphics.Canvas

class Explosion(
    private val x: Int,
    private val y: Int,
    private val frames: List<Bitmap>,
    private val frameTime: Int = 4 // số lần update() để chuyển frame
) {
    private var currentFrame = 0
    private var frameCounter = 0
    var isFinished = false
        private set

    fun update() {
        if (isFinished) return

        frameCounter++
        if (frameCounter >= frameTime) {
            frameCounter = 0
            currentFrame++
            if (currentFrame >= frames.size) {
                isFinished = true
            }
        }
    }

    fun draw(canvas: Canvas) {
        if (!isFinished && currentFrame < frames.size) {
            val bmp = frames[currentFrame]
            val drawX = (x - bmp.width / 2).toFloat()
            val drawY = (y - bmp.height / 2).toFloat()
            canvas.drawBitmap(bmp, drawX, drawY, null)
        }
    }

    fun reset() {
        currentFrame = 0
        frameCounter = 0
        isFinished = false
    }
}
