package com.example.chickenshooter

import android.content.Context
import android.graphics.Canvas
import android.view.MotionEvent

abstract class BaseLevel(
    val context: Context,
    val gameView: GameView,
    val player: Player
) {
    abstract fun initLevel()
    abstract fun update()
    abstract fun draw(canvas: Canvas)
    abstract fun onTouch(event: MotionEvent)
    open fun showVictory() {}
}