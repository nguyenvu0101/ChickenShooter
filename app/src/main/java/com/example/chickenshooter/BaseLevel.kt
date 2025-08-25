package com.example.chickenshooter.levels

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import com.example.chickenshooter.*

abstract class BaseLevel(
    val context: Context,
    val player: Player,
    val bulletBitmap: Bitmap,
    val itemBitmaps: List<Bitmap>
) {
    open var pickedGunMode: GunMode? = null
    abstract fun update(bullets: MutableList<Bullet>)
    abstract fun draw(canvas: Canvas, bullets: List<Bullet>)
    abstract fun isCompleted(): Boolean
    abstract fun reset()
    abstract fun getBackground(): Bitmap
    abstract fun getLives(): Int
}