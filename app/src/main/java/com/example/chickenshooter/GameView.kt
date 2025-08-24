package com.example.chickenshooter

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.example.chickenshooter.utils.CollisionUtils

enum class GunMode {
    NORMAL,      // 1 viên thường
    FAST,        // 1 viên, bắn nhanh
    TRIPLE_PARALLEL, // 3 viên song song
    TRIPLE_SPREAD    // 3 viên tỏa
}

class GameView(context: Context) : SurfaceView(context), SurfaceHolder.Callback {
    private val thread: GameThread
    private val playerBitmap = BitmapFactory.decodeResource(resources, R.drawable.player)
    private val chickenBitmaps = listOf(
        BitmapFactory.decodeResource(resources, R.drawable.chicken1),
        BitmapFactory.decodeResource(resources, R.drawable.chicken2),
        BitmapFactory.decodeResource(resources, R.drawable.chicken3),
        BitmapFactory.decodeResource(resources, R.drawable.chicken4)
    )
    private val bulletBitmap = BitmapFactory.decodeResource(resources, R.drawable.bullet)
    private val backgroundBitmap = BitmapFactory.decodeResource(resources, R.drawable.background)
    private val itemBitmaps = listOf(
        BitmapFactory.decodeResource(resources, R.drawable.item_fast),      // 0: bắn nhanh
        BitmapFactory.decodeResource(resources, R.drawable.item_parallel),  // 1: 3 viên song song
        BitmapFactory.decodeResource(resources, R.drawable.item_spread)     // 2: 3 viên tỏa
    )

    private lateinit var player: Player
    private val chickens = mutableListOf<Chicken>()
    private val bullets = mutableListOf<Bullet>()
    private val items = mutableListOf<Item>()

    // Gun mode
    private var gunMode = GunMode.NORMAL
    private var gunModeTimer = 0
    private val gunModeDuration = 8 * 60 // hiệu lực 8s

    // Auto shoot variables
    private var autoShootCounter = 0
    private val autoShootIntervalNormal = 10
    private val autoShootIntervalFast = 5

    // Level & logic
    private var level = 1
    private val maxLevel = 4
    private var levelTimer = 0
    private val levelDuration = 60 * 60 // 1 phút = 60 giây * 60fps
    private var isLevelChanging = false
    private var levelChangeCounter = 0
    private val levelChangeDuration = 90 // frame chuyển màn

    // Player lives
    private var lives = 3
    private var isPlayerHit = false
    private var hitCooldown = 0
    private val hitCooldownDuration = 40 // chống mất mạng liên tục khi va chạm

    // Enemy spawn variables (tăng độ khó dần)
    private var spawnCooldown = 0
    private val spawnIntervals = listOf(55, 40, 28, 16)
    private val chickenSpeeds = listOf(5, 7, 9, 12)
    private val chickenHPs = listOf(3, 5, 7, 10) // máu mỗi màn
    private val chickenMoveTypes = listOf(0, 1, 2, 3)
    private lateinit var scaledChickenBitmap: Bitmap

    init {
        holder.addCallback(this)
        thread = GameThread(holder, this)
        isFocusable = true
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        val centerX = width / 2 - playerBitmap.width / 2
        val bottomY = height - playerBitmap.height - 30
        player = Player(centerX, bottomY, playerBitmap)
        updateScaledChickenBitmap()
        startLevel(1)
        thread.running = true
        thread.start()
    }

    private fun updateScaledChickenBitmap() {
        val currentChickenBitmap = chickenBitmaps[(level - 1).coerceIn(0, chickenBitmaps.size - 1)]
        scaledChickenBitmap = Bitmap.createScaledBitmap(
            currentChickenBitmap,
            currentChickenBitmap.width *2/5,
            currentChickenBitmap.height *2/5,
            true
        )
    }

    fun startLevel(newLevel: Int) {
        level = newLevel
        updateScaledChickenBitmap()
        levelTimer = 0
        spawnCooldown = 0
        autoShootCounter = 0
        chickens.clear()
        bullets.clear()
        items.clear()
        lives = 3
        isLevelChanging = true
        levelChangeCounter = 0
        isPlayerHit = false
        hitCooldown = 0
        gunMode = GunMode.NORMAL
        gunModeTimer = 0
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        var retry = true
        thread.running = false
        while (retry) {
            try {
                thread.join()
                retry = false
            } catch (e: InterruptedException) {
                // retry
            }
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

    fun update() {
        if (isLevelChanging) {
            levelChangeCounter++
            if (levelChangeCounter >= levelChangeDuration) {
                isLevelChanging = false
            }
            return
        }
        if (lives <= 0) return

        // Đạn tự động
        val interval = when (gunMode) {
            GunMode.FAST -> autoShootIntervalFast
            else -> autoShootIntervalNormal
        }
        autoShootCounter++
        if (autoShootCounter >= interval) {
            val bulletY = player.y
            val center = player.x + playerBitmap.width / 2 - bulletBitmap.width / 2
            val offset = 40
            when (gunMode) {
                GunMode.NORMAL, GunMode.FAST -> {
                    bullets.add(Bullet(center, bulletY, bulletBitmap, 30, 2, 270.0))
                }
                GunMode.TRIPLE_PARALLEL -> {
                    bullets.add(Bullet(center, bulletY, bulletBitmap, 30, 2, 270.0))
                    bullets.add(Bullet(center - offset, bulletY, bulletBitmap, 30, 2, 270.0))
                    bullets.add(Bullet(center + offset, bulletY, bulletBitmap, 30, 2, 270.0))
                }
                GunMode.TRIPLE_SPREAD -> {
                    bullets.add(Bullet(center, bulletY, bulletBitmap, 30, 2, 270.0))   // giữa
                    bullets.add(Bullet(center, bulletY, bulletBitmap, 30, 2, 250.0))   // lệch trái
                    bullets.add(Bullet(center, bulletY, bulletBitmap, 30, 2, 290.0))   // lệch phải
                }
            }
            autoShootCounter = 0
        }

        // Cập nhật đạn
        bullets.forEach { it.update() }
        bullets.removeAll { it.y < 0 || it.x < 0 || it.x > width }

        // Sinh gà liên tục trong mỗi màn
        spawnCooldown++
        val spawnInterval = if (level - 1 < spawnIntervals.size) spawnIntervals[level - 1] else 16
        val chickenSpeed = if (level - 1 < chickenSpeeds.size) chickenSpeeds[level - 1] else 14
        val chickenHp = if (level - 1 < chickenHPs.size) chickenHPs[level - 1] else 10
        if (spawnCooldown >= spawnInterval) {
            val randomX = (0..(width - scaledChickenBitmap.width)).random()
            val moveType = chickenMoveTypes.random()
            chickens.add(Chicken(randomX, 0, scaledChickenBitmap, chickenSpeed, moveType, chickenHp))
            spawnCooldown = 0
        }

        // Cập nhật gà
        chickens.forEach { it.update() }
        chickens.removeAll { it.y > height }

        // Cập nhật vật phẩm rơi
        items.forEach { it.update() }
        items.removeAll { it.y > height }

        // Kiểm tra va chạm đạn-gà
        val deadChickens = mutableListOf<Chicken>()
        val usedBullets = mutableListOf<Bullet>()
        for (chicken in chickens) {
            for (bullet in bullets) {
                if (CollisionUtils.isColliding(chicken.getRect(), bullet.getRect())) {
                    chicken.hp -= bullet.damage
                    usedBullets.add(bullet)
                    if (chicken.hp <= 0) {
                        deadChickens.add(chicken)
                        // 10% rơi vật phẩm, chọn ngẫu nhiên loại
                        if ((0..99).random() < 10) {
                            val itemType = (0..2).random()
                            items.add(Item(chicken.x, chicken.y, itemBitmaps[itemType], itemType))
                        }
                    }
                }
            }
        }
        chickens.removeAll(deadChickens)
        bullets.removeAll(usedBullets)

        // Kiểm tra va chạm gà-player (mất mạng)
        if (!isPlayerHit) {
            for (chicken in chickens) {
                if (CollisionUtils.isColliding(chicken.getRect(), player.getRect())) {
                    lives--
                    isPlayerHit = true
                    hitCooldown = 0
                    break
                }
            }
        } else {
            hitCooldown++
            if (hitCooldown >= hitCooldownDuration) {
                isPlayerHit = false
            }
        }

        // Kiểm tra player ăn vật phẩm
        val collectedItems = items.filter { CollisionUtils.isColliding(it.getRect(), player.getRect()) }
        for (item in collectedItems) {
            when (item.type) {
                0 -> gunMode = GunMode.FAST
                1 -> gunMode = GunMode.TRIPLE_PARALLEL
                2 -> gunMode = GunMode.TRIPLE_SPREAD
            }
            gunModeTimer = 0
        }
        items.removeAll(collectedItems)

        // Đếm thời gian hiệu lực nâng cấp
        if (gunMode != GunMode.NORMAL) {
            gunModeTimer++
            if (gunModeTimer > gunModeDuration) {
                gunMode = GunMode.NORMAL
            }
        }

        // Đếm thời gian màn chơi, qua màn khi đủ thời gian
        levelTimer++
        if (levelTimer >= levelDuration) {
            if (level < maxLevel && lives > 0) {
                startLevel(level + 1)
            } else if (level >= maxLevel && lives > 0) {
                isLevelChanging = true
            }
        }
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        canvas.drawBitmap(backgroundBitmap, null, canvas.clipBounds, Paint())
        player.draw(canvas)
        chickens.forEach { it.draw(canvas) }
        bullets.forEach { it.draw(canvas) }
        items.forEach { it.draw(canvas) }

        // Hiển thị thông tin màn chơi và số mạng
        val paint = Paint()
        paint.color = Color.WHITE
        paint.textSize = 48f
        paint.isFakeBoldText = true
        //canvas.drawText("Màn: $level/4", 40f, 70f, paint)
        //canvas.drawText("Mạng: $lives", 40f, 130f, paint)
// Vẽ số lượng mạng bằng icon máy bay
        val lifeIcon = Bitmap.createScaledBitmap(playerBitmap, 60, 60, true)
        for (i in 0 until lives) {
            canvas.drawBitmap(lifeIcon, 40f + i * 70, 80f, paint)
        }
        // Nếu bị va chạm, có thể nháy nhân vật để báo hiệu mất mạng
        if (isPlayerHit && (levelTimer % 10 < 5)) {
            paint.color = Color.RED
            paint.textSize = 48f
            canvas.drawText("Bạn bị thương!", width / 2f - 130, height / 2f, paint)
        }

        // Nếu đang chuyển màn
        if (isLevelChanging) {
            paint.textSize = 80f
            paint.color = Color.YELLOW
            paint.textAlign = Paint.Align.CENTER
            val msg = when {
                lives <= 0 -> "GAME OVER"
                level > maxLevel -> "CHÚC MỪNG BẠN!"
                else -> "Level $level"
            }
            canvas.drawText(msg, width / 2f, height / 2f, paint)
            paint.textAlign = Paint.Align.LEFT
        }

        // Hiển thị hiệu ứng nâng cấp
        if (gunMode != GunMode.NORMAL) {
            paint.color = Color.GREEN
            paint.textSize = 40f
            val text = when (gunMode) {
                GunMode.FAST -> ""
                GunMode.TRIPLE_PARALLEL -> ""
                GunMode.TRIPLE_SPREAD -> ""
                else -> ""
            }
            canvas.drawText(text, width - 500f, 120f, paint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_MOVE) {
            val newX = event.x.toInt() - playerBitmap.width / 2
            player.x = newX.coerceIn(0, width - playerBitmap.width)
        }
        return true
    }

    // Thread game riêng
    inner class GameThread(private val surfaceHolder: SurfaceHolder, private val gameView: GameView) : Thread() {
        var running = false
        override fun run() {
            while (running) {
                val canvas: Canvas? = surfaceHolder.lockCanvas()
                if (canvas != null) {
                    synchronized(surfaceHolder) {
                        gameView.update()
                        gameView.draw(canvas)
                    }
                    surfaceHolder.unlockCanvasAndPost(canvas)
                }
                sleep(16) // ~60fps
            }
        }
    }
}