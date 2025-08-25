package com.example.chickenshooter.utils

import android.graphics.Rect

object CollisionUtils {
    fun isColliding(a: Rect, b: Rect): Boolean {
        return Rect.intersects(a, b)
    }
}