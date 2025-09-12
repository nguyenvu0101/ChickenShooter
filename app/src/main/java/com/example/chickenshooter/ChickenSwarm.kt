package com.example.chickenshooter

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import kotlin.math.ceil
import kotlin.math.min
import kotlin.random.Random

class ChickenSwarm(
    private val context: Context,
    private val numChickens: Int,
    private val chickenBitmap: Bitmap,
    private val screenWidth: Int,
    private val screenHeight: Int,
    private val swarmSpeed: Float = 4f
) {
    val chickens: MutableList<Chicken> = mutableListOf()
    private var dir = 1

    init {
        spawn()
    }

    private fun spawn() {
        val cols = min(numChickens, 6)
        val rows = ceil(numChickens / cols.toFloat()).toInt()
        val spacingX = chickenBitmap.width + 20
        val spacingY = chickenBitmap.height + 20
        val totalWidth = cols * spacingX
        val offsetX = ((screenWidth - totalWidth) / 2).coerceAtLeast(0)
        val startY = 80

        var created = 0
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                if (created >= numChickens) break
                val x = (offsetX + c * spacingX).toFloat()
                val y = (startY + r * spacingY).toFloat()

                val chicken = Chicken(
                    x = x,
                    y = y,
                    bitmap = chickenBitmap,
                    speed = 0f,   // Swarm điều khiển di chuyển
                    moveType = 0,
                    hp = 3,
                    screenWidth = screenWidth,
                    screenHeight = screenHeight
                )

                chickens.add(chicken)
                created++
            }
        }
    }

    fun update(playerX: Float, playerY: Float) {
        if (chickens.isEmpty()) return

        // di chuyển ngang
        for (chicken in chickens) {
            chicken.x += swarmSpeed * dir
        }

        val hitLeft = chickens.minOf { it.x } <= 0f
        val hitRight = chickens.maxOf { it.x + chickenBitmap.width } >= screenWidth.toFloat()

        if (hitLeft || hitRight) {
            dir *= -1
            val drop = chickenBitmap.height / 2f
            chickens.forEach { it.y += drop }
        }

        chickens.forEach { it.update(playerX, playerY) }
    }

    fun draw(canvas: Canvas) {
        chickens.forEach { it.draw(canvas) }
    }

    fun isEmpty(): Boolean = chickens.isEmpty()

    /**
     * Hàm dropLoot: gọi khi có gà chết
     */
    fun dropLoot(
        deadChickens: List<Chicken>,
        items: MutableList<Item>,
        shields: MutableList<Shield>,
        manaBitmap: Bitmap,
        itemBitmaps: List<Bitmap>,
        scaledShieldBitmap: Bitmap,
        spawnCoin: (x: Int, y: Int, w: Int, h: Int) -> Unit
    ) {
        for (chicken in deadChickens) {
            // Drop shield 30%
            if ((0..99).random() < 30) {
                shields.add(Shield(chicken.x.toInt(), chicken.y.toInt(), scaledShieldBitmap, 5))
            }
            // Drop item 10%
            if ((0..99).random() < 10) {
                val itemType = (0..2).random()
                items.add(
                    Item(
                        chicken.x.toInt(),
                        chicken.y.toInt(),
                        itemBitmaps[itemType],
                        ItemType.values()[itemType],
                        12
                    )
                )
            }
            // Drop mana 35%
            if (Math.random() < 0.35) {
                items.add(
                    Item(
                        chicken.x.toInt(),
                        chicken.y.toInt(),
                        manaBitmap,
                        ItemType.MANA,
                        8
                    )
                )
            }
            // Drop coin (dùng callback từ Level1)
            spawnCoin(
                chicken.x.toInt(),
                chicken.y.toInt(),
                chicken.bitmap.width,
                chicken.bitmap.height
            )
        }
    }
}
