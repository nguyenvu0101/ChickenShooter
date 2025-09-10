package com.example.chickenshooter

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.Paint
//class Player(
//    var x: Int,
//    var y: Int,
//    var bitmap: Bitmap ,
//    var gold : Int = 0
//) {
//    fun getRect(): android.graphics.Rect =
//        android.graphics.Rect(x, y, x + bitmap.width, y + bitmap.height)
//    fun draw(canvas: Canvas) {
//        canvas.drawBitmap(bitmap, x.toFloat(), y.toFloat(), null)
//    }
//}

class Player(
    var x: Int,
    var y: Int,
    var bitmap: Bitmap,
    var gold: Int = 0
) {
    var hasShield: Boolean = false
    var shieldEndTime: Long = 0L

    fun getRect(): android.graphics.Rect =
        android.graphics.Rect(x, y, x + bitmap.width, y + bitmap.height)

    fun draw(canvas: Canvas) {
        // Vẽ máy bay
        canvas.drawBitmap(bitmap, x.toFloat(), y.toFloat(), null)

        // Vẽ hiệu ứng khiên bảo vệ nếu đang có khiên
        if (hasShield) {
            val centerX = x + bitmap.width / 2f
            val centerY = y + bitmap.height / 2f
            val radius = (bitmap.width.coerceAtLeast(bitmap.height) * 0.7).toFloat()

            // Vòng tròn mờ viền xanh
            val strokePaint = Paint().apply {
                color = android.graphics.Color.argb(128, 0, 200, 255) // Xanh mờ 50%
                style = Paint.Style.STROKE
                strokeWidth = 10f
                isAntiAlias = true
            }
            canvas.drawCircle(centerX, centerY, radius, strokePaint)

            // Vùng sáng mờ bên trong
            val fillPaint = Paint().apply {
                color = android.graphics.Color.argb(40, 0, 200, 255) // Xanh rất mờ
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            canvas.drawCircle(centerX, centerY, radius - 4f, fillPaint)
        }
    }

    // Kích hoạt khiên: gọi khi nhặt item Shield
    fun activateShield(durationMs: Long) {
        hasShield = true
        shieldEndTime = System.currentTimeMillis() + durationMs
    }

    // Update mỗi frame/game tick
    fun update() {
        if (hasShield && System.currentTimeMillis() > shieldEndTime) {
            hasShield = false
        }
    }
}