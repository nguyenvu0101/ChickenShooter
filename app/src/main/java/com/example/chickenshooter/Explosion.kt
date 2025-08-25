package com.example.chickenshooter

import android.graphics.Bitmap
import android.graphics.Canvas

class Explosion(
    val x: Int,
    val y: Int,
    private val frames: List<Bitmap>,
    private val frameTime: Int = 4
) {
    private var currentFrame = 0
    private var frameCounter = 0
    var isFinished = false

    fun update() {
        frameCounter++
        if (frameCounter >= frameTime) {
            frameCounter = 0
            currentFrame++
            if (currentFrame >= frames.size) isFinished = true
        }
    }

    fun draw(canvas: Canvas) {
        if (!isFinished && currentFrame < frames.size)
            canvas.drawBitmap(frames[currentFrame], x.toFloat(), y.toFloat(), null)
    }
}