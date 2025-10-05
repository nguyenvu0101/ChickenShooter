package com.example.chickenshooter

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import kotlin.math.*
import kotlin.random.Random

enum class MoveType {
    DOWN,       // Thẳng xuống, đến gần player thì lên, dao động lên-xuống (spring)
    SINE,       // Lượn sóng sin trái-phải khi xuống
    ZIGZAG,     // Zigzag ngang khi xuống, tới gần player thì lên, dao động lên-xuống
    V,          // Hình răng cưa: xếp xen kẽ cao thấp, lên xuống cùng biên độ và chu kỳ
    SPIRAL,     // Xoắn ốc
    FIGURE8,    // Hình số 8
    BOUNCE      // Rơi xuống, đi ngang hết màn, sau đó đi lên
}

class Chicken(
    var x: Float,
    var y: Float,
    val bitmap: Bitmap,
    val speed: Float,
    val moveType: MoveType,
    var hp: Int,
    val screenWidth: Int,
    val screenHeight: Int
) {
    private var tick = 0
    private var lastShotTime = 0L
    private var shotInterval = Random.nextLong(2500, 5000)
    private var angle = 0f
    private var amplitude = Random.nextFloat() * 50 + 30 // 30–80
    private var frequency = Random.nextFloat() * 0.3f + 0.1f // 0.1–0.4
    private val originalX = x
    private val originalY = y
    private var direction = if (Random.nextBoolean()) 1 else -1
    private var hasShot = false

    // Dùng cho kiểu lên xuống (spring)
    private var phaseSpring = 1 // 1: lên, -1: xuống

    // Dùng cho zigzag
    private var zigzagDir = if (Random.nextBoolean()) 1 else -1

    // Dùng cho bounce
    private var bouncePhase = 0

    // Danh sách đạn
    val projectiles = mutableListOf<ChickenProjectile>()

    fun update(playerX: Float, playerY: Float) {
        tick++
        val currentTime = System.currentTimeMillis()

        // Bắn: mỗi gà chỉ bắn 1 viên trong đời
        if (!hasShot && y in 0f..screenHeight.toFloat()) {
            shoot(playerX, playerY)
            lastShotTime = currentTime
            hasShot = true
        }

        when (moveType) {
            MoveType.DOWN -> {
                // Lò xo lên xuống giữa minY và maxY (gần tới player)
                val minY = 0f
                val maxY = playerY - 120f
                y += phaseSpring * speed
                if (y < minY) {
                    y = minY
                    phaseSpring = 1
                }
                if (y > maxY) {
                    y = maxY
                    phaseSpring = -1
                }
            }
            MoveType.SINE -> {
                val minY = 0f
                val maxY = screenHeight - bitmap.height - 50f
                y += phaseSpring * speed        // Thêm dòng này thay vì y += speed
                x = originalX + sin(tick * frequency) * amplitude
                if (y < minY) {
                    y = minY
                    phaseSpring = 1
                }
                if (y > maxY) {
                    y = maxY
                    phaseSpring = -1
                }
            }
            MoveType.ZIGZAG -> {
                // Zigzag ngang + lên xuống như lò xo
                val minY = 0f
                val maxY = playerY - 120f
                y += phaseSpring * speed
                x += zigzagDir * speed * 1.1f
                if (x < 0f) {
                    x = 0f
                    zigzagDir = 1
                }
                if (x > screenWidth - bitmap.width) {
                    x = screenWidth - bitmap.width.toFloat()
                    zigzagDir = -1
                }
                if (y < minY) {
                    y = minY
                    phaseSpring = 1
                }
                if (y > maxY) {
                    y = maxY
                    phaseSpring = -1
                }
            }
            MoveType.V -> {
                // Nếu muốn cũng lên-xuống mãi thì sửa như SINE:
                val minY = 0f
                val maxY = screenHeight - bitmap.height - 50f
                y += phaseSpring * speed
                val period = 120f
                val amp = 60f
                x = originalX + amp * sin((tick + (originalY % period)) * 0.045f)
                if (y < minY) {
                    y = minY
                    phaseSpring = 1
                }
                if (y > maxY) {
                    y = maxY
                    phaseSpring = -1
                }
            }
            MoveType.SPIRAL -> {
                val minY = 0f
                val maxY = screenHeight - bitmap.height - 50f
                y += phaseSpring * speed * 0.7f
                angle += 0.1f
                x += cos(angle) * 3f
                if (y < minY) {
                    y = minY
                    phaseSpring = 1
                }
                if (y > maxY) {
                    y = maxY
                    phaseSpring = -1
                }
            }
            MoveType.FIGURE8 -> {
                val minY = 0f
                val maxY = screenHeight - bitmap.height - 50f
                y += phaseSpring * speed * 0.8f
                val t = tick * 0.045f
                x = originalX + sin(t) * amplitude + cos(t * 2) * amplitude * 0.5f
                if (y < minY) {
                    y = minY
                    phaseSpring = 1
                }
                if (y > maxY) {
                    y = maxY
                    phaseSpring = -1
                }
            }
            MoveType.BOUNCE -> {
                // Giai đoạn 1: rơi xuống gần đáy
                val bottomY = screenHeight * 0.85f - bitmap.height
                if (bouncePhase == 0 && y < bottomY) {
                    y += speed
                    x += direction * speed * 1.5f
                    if (x <= 0 || x >= screenWidth - bitmap.width) direction *= -1
                    if (y >= bottomY) {
                        bouncePhase = 1
                        direction = if (x < screenWidth / 2) 1 else -1
                    }
                } else if (bouncePhase == 1) {
                    // Đi ngang về mép đối diện
                    x += direction * speed * 1.5f
                    if ((direction > 0 && x >= screenWidth - bitmap.width) ||
                        (direction < 0 && x <= 0f)
                    ) {
                        bouncePhase = 2
                    }
                } else if (bouncePhase == 2) {
                    // Đi lên, có thể lắc nhẹ
                    y -= speed
                    x += sin(tick * 0.07f) * 2.0f
                }
            }
        }

        // Giới hạn trong màn hình X (không giới hạn Y để gà có thể bay ra khỏi màn)
        x = x.coerceIn(0f, (screenWidth - bitmap.width).toFloat())

        // Cập nhật đạn
        val iterator = projectiles.iterator()
        while (iterator.hasNext()) {
            val projectile = iterator.next()
            projectile.update()
            if (projectile.y > screenHeight) {
                iterator.remove()
            }
        }
    }

    private fun shoot(playerX: Float, playerY: Float) {
        val centerX = x + bitmap.width / 2
        val centerY = y + bitmap.height
        if (projectiles.isNotEmpty()) return // chỉ 1 viên mỗi con gà

        if ((0..99).random() < 40) {
            // 50/50 EGG hoặc SHIT
            val type = if (Random.nextBoolean()) ProjectileType.EGG else ProjectileType.SHIT
            projectiles.add(ChickenProjectile(centerX, centerY, 0f, 8f, type))
        }
        // Còn lại 65% không bắn gì cả
    }

    fun getRect(): Rect = Rect(
        x.toInt(), y.toInt(),
        x.toInt() + bitmap.width, y.toInt() + bitmap.height
    )

    fun draw(canvas: Canvas) {
        canvas.drawBitmap(bitmap, x, y, null)
        projectiles.forEach { it.draw(canvas) }
    }

    // Không cho gà tự biến mất khi ra khỏi màn hình
    fun isOffScreen(): Boolean = false
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
        private const val PROJECTILE_SIZE = 50

        private val blackPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.BLACK
        }
        private val whitePaint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
        }

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
            val paint = when (type) {
                ProjectileType.SHIT -> blackPaint
                ProjectileType.EGG -> whitePaint
            }
            canvas.drawCircle(x, y, 5f, paint)
        }
    }
}