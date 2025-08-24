package com.example.chickenshooter.utils

import android.graphics.Rect

object CollisionUtils {
    fun isColliding(rect1: Rect, rect2: Rect): Boolean {
        return Rect.intersects(rect1, rect2)
    }
}