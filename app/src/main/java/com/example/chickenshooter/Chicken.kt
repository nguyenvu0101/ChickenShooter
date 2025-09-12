package com.example.chickenshooter

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import kotlin.math.*
import kotlin.random.Random

class Chicken(
    var x: Float,
    var y: Float,
    val bitmap: Bitmap,
    val speed: Float,
    val moveType: Int,
    var hp: Int,
    val screenWidth: Int,
    val screenHeight: Int
) {
    private var tick = 0
    private var lastShotTime = 0L
    private var shotInterval = Random.nextLong(2000, 5000) // 2–5 giây
    private var angle = 0f
    private var amplitude = Random.nextFloat() * 50 + 30 // 30–80
    private var frequency = Random.nextFloat() * 0.3f + 0.1f // 0.1–0.4
    private var originalX = x
    private var direction = if (Random.nextBoolean()) 1 else -1

    // Danh sách đạn
    val projectiles = mutableListOf<ChickenProjectile>()

    fun update(playerX: Float, playerY: Float) {
        tick++
        val currentTime = System.currentTimeMillis()

        // Cập nhật vị trí
        when (moveType) {
            0 -> y += speed // thẳng xuống
            1 -> { // sine
                y += speed
                x = originalX + sin(tick * frequency) * amplitude
            }
            2 -> { // cosine
                y += speed
                x = originalX + cos(tick * frequency * 1.5f) * amplitude * 1.2f
            }
            3 -> { // zigzag
                y += speed
                if (tick % 60 == 0) direction *= -1
                x += direction * speed * 0.8f
            }
            4 -> { // spiral
                y += speed * 0.7f
                angle += 0.1f
                x += cos(angle) * 3f
            }
            5 -> { // erratic
                y += speed
                if (tick % 30 == 0) x += Random.nextFloat() * 40 - 20
            }
            6 -> { // follow
                y += speed
                val targetX = playerX - bitmap.width / 2
                val diffX = targetX - x
                x += diffX * 0.02f
            }
            7 -> { // figure-8
                y += speed * 0.8f
                val t = tick * 0.05f
                x = originalX + sin(t) * amplitude + cos(t * 2) * amplitude * 0.5f
            }
            8 -> { // bouncing
                y += speed
                x += direction * speed * 1.5f
                if (x <= 0 || x >= screenWidth - bitmap.width) direction *= -1
            }
            else -> y += speed
        }

        // Giới hạn trong màn hình
        x = x.coerceIn(0f, (screenWidth - bitmap.width).toFloat())

        // Bắn đạn
        if (currentTime - lastShotTime > shotInterval && y in 0f..screenHeight.toFloat()) {
            shoot(playerX, playerY)
            lastShotTime = currentTime
            shotInterval = Random.nextLong(2000, 5000) // reset random
        }

        // Cập nhật đạn (dùng iterator để tránh lag)
        val iterator = projectiles.iterator()
        while (iterator.hasNext()) {
            val projectile = iterator.next()
            projectile.update()
            if (projectile.y > screenHeight || projectile.y < 0) {
                iterator.remove()
            }
        }
    }

    private fun shoot(playerX: Float, playerY: Float) {
        val centerX = x + bitmap.width / 2
        val centerY = y + bitmap.height

        when (Random.nextInt(4)) {
            0 -> { // thẳng xuống
                projectiles.add(ChickenProjectile(centerX, centerY, 0f, 8f, ProjectileType.SHIT))
            }
            1 -> { // hướng về player
                val dx = playerX - centerX
                val dy = playerY - centerY
                val distance = sqrt(dx * dx + dy * dy)
                if (distance > 0) {
                    val speed = 6f
                    projectiles.add(
                        ChickenProjectile(
                            centerX, centerY,
                            (dx / distance) * speed,
                            (dy / distance) * speed,
                            ProjectileType.EGG
                        )
                    )
                }
            }
            2 -> { // spread 3 viên
                val baseSpeed = 7f
                for (i in -1..1) {
                    val angle = i * 0.3f
                    projectiles.add(
                        ChickenProjectile(
                            centerX, centerY,
                            sin(angle) * baseSpeed,
                            cos(angle) * baseSpeed,
                            ProjectileType.SHIT
                        )
                    )
                }
            }
            3 -> { // bắn theo góc tới player
                val dx = playerX - centerX
                val dy = playerY - centerY
                val targetAngle = atan2(dy, dx) // fix: y trước, x sau
                val speed = 5f
                projectiles.add(
                    ChickenProjectile(
                        centerX, centerY,
                        cos(targetAngle) * speed,
                        sin(targetAngle) * speed,
                        ProjectileType.EGG
                    )
                )
            }
        }
    }

    fun getRect(): Rect = Rect(
        x.toInt(), y.toInt(),
        x.toInt() + bitmap.width, y.toInt() + bitmap.height
    )

    fun draw(canvas: Canvas) {
        canvas.drawBitmap(bitmap, x, y, null)
        projectiles.forEach { it.draw(canvas) }
    }

    fun isOffScreen(): Boolean = y > screenHeight + bitmap.height
}

enum class ProjectileType { SHIT, EGG }

class ChickenProjectile(
    var x: Float,
    var y: Float,
    private val velocityX: Float,
    private val velocityY: Float,
    val type: ProjectileType
) {
    companion object {
        private var eggBitmap: Bitmap? = null
        private var shitBitmap: Bitmap? = null
        private const val PROJECTILE_SIZE = 50 // px

        fun init(resources: android.content.res.Resources) {
            if (eggBitmap == null) {
                val original = android.graphics.BitmapFactory.decodeResource(resources, R.drawable.egg_v1)
                eggBitmap = Bitmap.createScaledBitmap(original, PROJECTILE_SIZE, PROJECTILE_SIZE, true)
            }
            if (shitBitmap == null) {
                val original = android.graphics.BitmapFactory.decodeResource(resources, R.drawable.shit_v1)
                shitBitmap = Bitmap.createScaledBitmap(original, PROJECTILE_SIZE, PROJECTILE_SIZE, true)
            }
        }
    }

    fun update() {
        x += velocityX
        y += velocityY
    }

    fun getRect(): Rect {
        val bitmap = when (type) {
            ProjectileType.SHIT -> shitBitmap
            ProjectileType.EGG -> eggBitmap
        }
        return if (bitmap != null) {
            Rect(
                (x - bitmap.width / 2).toInt(),
                (y - bitmap.height / 2).toInt(),
                (x + bitmap.width / 2).toInt(),
                (y + bitmap.height / 2).toInt()
            )
        } else {
            Rect(x.toInt() - 5, y.toInt() - 5, x.toInt() + 5, y.toInt() + 5)
        }
    }

    fun draw(canvas: Canvas) {
        val bitmap = when (type) {
            ProjectileType.SHIT -> shitBitmap
            ProjectileType.EGG -> eggBitmap
        }
        if (bitmap != null) {
            canvas.drawBitmap(bitmap, x - bitmap.width / 2, y - bitmap.height / 2, null)
        } else {
            val paint = android.graphics.Paint().apply {
                color = when (type) {
                    ProjectileType.SHIT -> android.graphics.Color.BLACK
                    ProjectileType.EGG -> android.graphics.Color.WHITE
                }
            }
            canvas.drawCircle(x, y, 5f, paint)
        }
    }
}
