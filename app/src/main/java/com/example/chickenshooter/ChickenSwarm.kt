package com.example.chickenshooter

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

enum class SwarmPattern {
    HORIZONTAL, SIN_WAVE, CHICKEN_INVADERS
}

enum class SwarmPhase { START, MERGE, HORIZONTAL }

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
    private var tick = 0

    private var currentPattern: SwarmPattern = SwarmPattern.values().random()
    private var previousPattern: SwarmPattern? = null
    private var phase = SwarmPhase.START

    private val leftChickens = mutableListOf<Chicken>()
    private val rightChickens = mutableListOf<Chicken>()

    // Tuning params
    private val edgeMargin = 12f
    private val verticalSpacing = chickenBitmap.height + 8
    private val descendSpeed = 20f
    private val mergeLerp = 0.12f

    // --- Thêm biến quản lý thời gian ---
    private var lastPatternChange = System.currentTimeMillis()
    private val patternInterval = 5000L   // 5 giây đổi pattern

    // --- Time-based SinWave config ---
    private var lastUpdateNano: Long = System.nanoTime()
    private var totalTimeSec = 0f

    private var fallDurationSec = 5f   // gà rơi hết màn hình trong 2 giây
    private var waveCount = 3f         // số lần lượn trong suốt hành trình
    private var fallSpeedPxPerSec = 0f
    private var waveAngularSpeed = 0.0 // rad/s

    // Với đàn trái → bay từ mép trái sang phải
    val centerLeftX = screenWidth * 0.0f    // bắt đầu sát mép trái
    val amplitudeLeft = screenWidth * 0.5f  // dao động nửa màn hình

    // Với đàn phải → bay từ mép phải sang trái
    val centerRightX = screenWidth * 1.0f   // bắt đầu sát mép phải
    val amplitudeRight = screenWidth * 0.5f // dao động nửa màn hình



    init {
        prepareChickenInvaders()
    }

    private fun prepareChickenInvaders() {
        leftChickens.clear()
        rightChickens.clear()
        chickens.clear()

        val half = numChickens / 2
        for (i in 0 until half) {
            val x = edgeMargin
            val y = -(i * verticalSpacing + chickenBitmap.height.toFloat())
            val ch = Chicken(x, y, chickenBitmap, 0f, 0, 3, screenWidth, screenHeight)
            leftChickens.add(ch)
            chickens.add(ch)
        }
        for (i in 0 until (numChickens - half)) {
            val x = (screenWidth - chickenBitmap.width - edgeMargin)
            val y = -(i * verticalSpacing + chickenBitmap.height.toFloat())
            val ch = Chicken(x, y, chickenBitmap, 0f, 0, 3, screenWidth, screenHeight)
            rightChickens.add(ch)
            chickens.add(ch)
        }

        phase = SwarmPhase.START
    }

    fun update(playerX: Float, playerY: Float) {
        if (chickens.isEmpty()) return

        tick++

        // --- Dùng thời gian thực thay vì tick ---
        val now = System.currentTimeMillis()
        if (now - lastPatternChange >= patternInterval) {
            currentPattern = SwarmPattern.values().random()
            lastPatternChange = now
        }

        // Nếu đổi sang ChickenInvaders thì reset đội hình
        if (currentPattern != previousPattern) {
            when (currentPattern) {
                SwarmPattern.CHICKEN_INVADERS -> prepareChickenInvaders()
                SwarmPattern.SIN_WAVE -> prepareDoubleSinWave()
                SwarmPattern.HORIZONTAL -> { /* prepareHorizontal() nếu cần */ }
            }
            previousPattern = currentPattern
        }

        when (currentPattern) {
            SwarmPattern.HORIZONTAL -> moveCircular()
            SwarmPattern.SIN_WAVE -> moveDoubleSinWave()
            SwarmPattern.CHICKEN_INVADERS -> moveChickenInvaders()
        }

        chickens.forEach { it.update(playerX, playerY) }
    }

    private fun moveAfterChickenInvaders() {
        for (chicken in chickens) chicken.x += swarmSpeed * dir
        val hitLeft = chickens.minOf { it.x } <= 0f
        val hitRight = chickens.maxOf { it.x + chickenBitmap.width } >= screenWidth.toFloat()
        if (hitLeft || hitRight) {
            dir *= -1
            val drop = chickenBitmap.height / 2f
            chickens.forEach { it.y += drop }
        }
    }

    private fun moveZigZag() {
        // tăng tốc ngang để biên độ rộng hơn
        val horizontalSpeed = swarmSpeed * 1.5f

        for (chicken in chickens) {
            chicken.x += horizontalSpeed * dir
        }

        val hitLeft = chickens.minOf { it.x } <= 0f
        val hitRight = chickens.maxOf { it.x + chickenBitmap.width } >= screenWidth.toFloat()

        if (hitLeft || hitRight) {
            dir *= -1
            // tụt xuống ít hơn để chậm rãi hơn
            val drop = chickenBitmap.height / 4f   // nhỏ hơn 1/2 chiều cao
            chickens.forEach { it.y += drop }
        }
    }

    // 2 đàn riêng
    private val leftWave = mutableListOf<Chicken>()
    private val rightWave = mutableListOf<Chicken>()

    private fun prepareDoubleSinWave() {
        chickens.clear()
        leftWave.clear()
        rightWave.clear()

        // --- Reset time & tính tốc độ rơi ---
        fallSpeedPxPerSec = screenHeight / fallDurationSec
        waveAngularSpeed = 2.0 * Math.PI * (waveCount / fallDurationSec)
        lastUpdateNano = System.nanoTime()
        totalTimeSec = 0f

        val half = numChickens / 2
        val verticalSpacing = chickenBitmap.height + 20

        for (i in 0 until half) {
            // đàn trái
            val ch = Chicken(
                x = chickenBitmap.width.toFloat(),
                y = -(i * (chickenBitmap.height + 20)).toFloat(),
                bitmap = chickenBitmap,
                speed = 0f,
                moveType = 0,
                hp = 3,
                screenWidth = screenWidth,
                screenHeight = screenHeight
            )
            leftWave.add(ch)
            chickens.add(ch)
        }
        for (i in 0 until half) {
            // đàn phải
            val ch = Chicken(
                x = (screenWidth - chickenBitmap.width).toFloat(),
                y = -(i * (chickenBitmap.height + 20)).toFloat(),
                bitmap = chickenBitmap,
                speed = 0f,
                moveType = 0,
                hp = 3,
                screenWidth = screenWidth,
                screenHeight = screenHeight
            )
            rightWave.add(ch)
            chickens.add(ch)
        }
    }

    private val indexPhaseOffset = 0.4f   // lệch pha giữa các gà (rad)

    private fun moveDoubleSinWave() {
        val nowNano = System.nanoTime()
        val dtSec = (nowNano - lastUpdateNano) / 1_000_000_000f
        lastUpdateNano = nowNano
        totalTimeSec += dtSec

        val centerLeftX = screenWidth * 0.25f
        val centerRightX = screenWidth * 0.75f

        for ((index, chicken) in leftWave.withIndex()) {
            chicken.y += fallSpeedPxPerSec * dtSec
            val phase = waveAngularSpeed * totalTimeSec + index * indexPhaseOffset
            // dao động từ 0 đến 1 màn hình
            chicken.x = (screenWidth / 2f) * (1 + sin(phase).toFloat())
        }

        for ((index, chicken) in rightWave.withIndex()) {
            chicken.y += fallSpeedPxPerSec * dtSec
            val phase = waveAngularSpeed * totalTimeSec + index * indexPhaseOffset
            // đảo ngược sin để đi ngược chiều
            chicken.x = screenWidth - (screenWidth / 2f) * (1 + sin(phase).toFloat())
        }


        // Khi tất cả rơi hết -> clear hoặc respawn
        if (chickens.all { it.y > screenHeight + chickenBitmap.height }) {
            // Ví dụ: respawn lại
            // prepareDoubleSinWave()
        }
    }


    // --- move circular code
    private var angle = 0.0
    private var globalProgress = 0f   // tiến trình chung
    private var enterFromLeft = Random.nextBoolean()  // random trái/phải

    // easing mượt mà
    private fun easeInOutQuad(t: Float): Float {
        return if (t < 0.5f) {
            2f * t * t
        } else {
            1f - ((-2f * t + 2f).let { it * it }) / 2f
        }
    }

    private fun moveCircular() {
        angle += 0.05

        // tăng dần tiến trình
        if (globalProgress < 1f) {
            globalProgress += 0.01f
        }

        val centerX = screenWidth / 2f
        val centerY = screenHeight / 3f
        val radiusX = screenWidth / 3f
        val radiusY = 80f

        chickens.forEachIndexed { index, chicken ->
            val theta = angle + index * (Math.PI / 12)

            // vị trí mục tiêu trên quỹ đạo
            val targetX = (centerX + radiusX * kotlin.math.cos(theta)).toFloat()
            val targetY = (centerY + radiusY * kotlin.math.sin(theta)).toFloat()

            // spawn trễ dần theo index
            val delay = index * 0.1f
            val localProgress = ((globalProgress - delay).coerceIn(0f, 1f))
            val eased = easeInOutQuad(localProgress)

            // xuất phát từ trái hoặc phải
            val startX = if (enterFromLeft) {
                -chicken.bitmap.width.toFloat() - index * 40f  // ngoài màn hình trái
            } else {
                screenWidth + index * 40f  // ngoài màn hình phải
            }
            val startY = targetY  // cùng cao độ với mục tiêu

            // lerp từ start -> target
            chicken.x = startX + (targetX - startX) * eased
            chicken.y = startY + (targetY - startY) * eased
        }
    }


    // --- move chickenInvaders code
    private fun moveChickenInvaders() {
        when (phase) {
            SwarmPhase.START -> {
                // 1) Cột trái đi dọc xuống, giữ x cố định
                val leftX = edgeMargin
                leftChickens.forEachIndexed { idx, ch ->
                    ch.x = leftX
                    ch.y += descendSpeed
                }

                // 2) Cột phải đi dọc xuống, giữ x cố định
                val rightX = (screenWidth - chickenBitmap.width - edgeMargin)
                rightChickens.forEachIndexed { idx, ch ->
                    ch.x = rightX
                    ch.y += descendSpeed
                }

                // Kiểm tra "đã chạm đáy" bằng bottom coordinate
                val leftBottom = leftChickens.maxOfOrNull { it.y + chickenBitmap.height } ?: Float.MIN_VALUE
                val rightBottom = rightChickens.maxOfOrNull { it.y + chickenBitmap.height } ?: Float.MIN_VALUE

                // Nếu cả hai cột đều đã chạm (hoặc vượt) đáy màn hình -> chuyển MERGE
                if (leftBottom >= screenHeight.toFloat() && rightBottom >= screenHeight.toFloat()) {
                    phase = SwarmPhase.MERGE
                }
            }

            SwarmPhase.MERGE -> {
                // Trước khi xếp hàng, sort chickens theo x để đảm bảo thứ tự trái->phải
                chickens.sortBy { it.x }

                val spacingX = chickenBitmap.width + 10
                val midX = (screenWidth / 2f) - (chickens.size * spacingX) / 2f
                val targetYUp = screenHeight * 1f / 3f // bay lên tới ~2/3 màn hình (từ trên)

                chickens.forEachIndexed { i, chicken ->
                    val desiredX = midX + i * spacingX
                    // Lerp tới vị trí mong muốn (mượt)
                    chicken.x += (desiredX - chicken.x) * mergeLerp
                    chicken.y += (targetYUp - chicken.y) * mergeLerp
                }

                // Nếu tất cả gần target -> chuyển phase HORIZONTAL
                if (chickens.all { abs(it.y - targetYUp) < 2f }) {
                    phase = SwarmPhase.HORIZONTAL
                }
            }

            SwarmPhase.HORIZONTAL -> moveAfterChickenInvaders()
        }
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
