package com.example.chickenshooter.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory

object SpriteUtils {

    /**
     * Cắt sprite sheet thành danh sách frame Bitmap.
     *
     * @param context   Context của app
     * @param resId     ID resource của sprite sheet (ví dụ: R.drawable.explosion)
     * @param rows      Số hàng trong sprite sheet
     * @param cols      Số cột trong sprite sheet
     * @param scale     Tỉ lệ scale (1.0 = giữ nguyên, 0.5 = nhỏ một nửa, ...)
     */
    fun splitSpriteSheet(
        context: Context,
        resId: Int,
        rows: Int,
        cols: Int,
        scale: Float = 1.0f
    ): List<Bitmap> {

        val bitmap = BitmapFactory.decodeResource(context.resources, resId)
        val frameWidth = bitmap.width / cols
        val frameHeight = bitmap.height / rows

        val frames = mutableListOf<Bitmap>()

        for (row in 0 until rows) {
            for (col in 0 until cols) {
                val frame = Bitmap.createBitmap(
                    bitmap,
                    col * frameWidth,
                    row * frameHeight,
                    frameWidth,
                    frameHeight
                )

                // Scale nếu cần
                val scaled = if (scale != 1.0f) {
                    Bitmap.createScaledBitmap(
                        frame,
                        (frameWidth * scale).toInt(),
                        (frameHeight * scale).toInt(),
                        true
                    )
                } else {
                    frame
                }

                frames.add(scaled)
            }
        }

        // Giải phóng bộ nhớ bitmap gốc
        bitmap.recycle()

        return frames
    }
}
