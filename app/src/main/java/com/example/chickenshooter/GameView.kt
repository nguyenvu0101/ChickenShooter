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
import android.media.SoundPool
import com.example.chickenshooter.levels.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

enum class GunMode {
    NORMAL, FAST, TRIPLE_PARALLEL, TRIPLE_SPREAD
}

class GameView(context: Context, private val backgroundId: Int, planeId: Int)
    : SurfaceView(context), SurfaceHolder.Callback {

    private val thread: GameThread
    private val playerBitmap = BitmapFactory.decodeResource(resources, planeId)
    private val bulletBitmap = BitmapFactory.decodeResource(resources, R.drawable.bullet)
    private val itemBitmaps = listOf(
        BitmapFactory.decodeResource(resources, R.drawable.item_fast),
        BitmapFactory.decodeResource(resources, R.drawable.item_parallel),
        BitmapFactory.decodeResource(resources, R.drawable.item_spread)
    )
    private val coinBitmap = BitmapFactory.decodeResource(resources, R.drawable.coin)

    private lateinit var player: Player
    private val bullets = mutableListOf<Bullet>()

    private lateinit var soundPool: SoundPool
    private var gunshotSoundId: Int = 0

    private var level = 1
    private val maxLevel = 4
    private lateinit var currentLevel: BaseLevel

    // Gun mode
    private var gunMode = GunMode.NORMAL
    private var gunModeTimer = 0
    private val gunModeDuration = 8 * 60
    private var autoShootCounter = 0
    private val autoShootIntervalNormal = 10
    private val autoShootIntervalFast = 5

    private var isLevelChanging = false
    private var levelChangeCounter = 0
    private val levelChangeDuration = 90

    // Coins
    private val userRepo = UserRepoRTDB()
    private var localCoin = 0
    private var coinBeforePlay = 0
    init {
        holder.addCallback(this)
        thread = GameThread(holder, this)
        isFocusable = true
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        val centerX = width / 2 - playerBitmap.width / 2
        val bottomY = height - playerBitmap.height - 30
        player = Player(centerX, bottomY, playerBitmap)

        soundPool = SoundPool.Builder().setMaxStreams(5).build()
        gunshotSoundId = soundPool.load(context, R.raw.laser, 1)

        if (isOfflineMode()) {
            val prefs = context.getSharedPreferences("game", Context.MODE_PRIVATE)
            localCoin = prefs.getLong("coins", 0L).toInt()
            coinBeforePlay = localCoin
            startLevel(1)
            thread.running = true
            thread.start()
        } else {
            Firebase.auth.currentUser?.uid?.let { uid ->
                userRepo.loadOnlineProfileOnce(uid,
                    onDone = { name: String, coins: Long ->
                        localCoin = coins.toInt()
                        coinBeforePlay = coins.toInt() // <-- lưu coin trước khi chơi
                        startLevel(1)
                        thread.running = true
                        thread.start()
                    },
                    onErr = { msg ->
                        localCoin = 0
                        coinBeforePlay = 0
                        startLevel(1)
                        thread.running = true
                        thread.start()
                    }
                )
            }
        }
    }
    fun startLevel(newLevel: Int) {
        level = newLevel
        currentLevel = when (level) {
            1 -> Level1(context, player, bulletBitmap, itemBitmaps, coinBitmap, backgroundId)
            2 -> Level2(context, player, bulletBitmap, itemBitmaps, coinBitmap, backgroundId)
            3 -> Level3(context, player, bulletBitmap, itemBitmaps, coinBitmap, backgroundId)
            4 -> Level4(context, player, bulletBitmap, itemBitmaps, coinBitmap, backgroundId)
            else -> Level1(context, player, bulletBitmap, itemBitmaps, coinBitmap, backgroundId)
        }

        currentLevel.setScreenSize(width, height)

        // khi nhặt coin
        currentLevel.onCoinCollected = { amount: Int ->
            localCoin += amount
//            if (isOfflineMode()) {
//                // Lưu SharedPreferences
//                val prefs = context.getSharedPreferences("game", Context.MODE_PRIVATE)
//                prefs.edit().putLong("coins", localCoin.toLong()).apply()
//            } else {
//                // Lưu lên Firebase (chỉ cần gọi thế này là đúng)
//                Firebase.auth.currentUser?.uid?.let { uid ->
//                    userRepo.addCoins(uid, amount.toLong())
//                }
//            }
        }
        currentLevel.reset()
        bullets.clear()
        gunMode = GunMode.NORMAL
        gunModeTimer = 0
        autoShootCounter = 0
        isLevelChanging = true
        levelChangeCounter = 0
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        var retry = true
        thread.running = false
        soundPool.release()
        while (retry) {
            try {
                thread.join()
                retry = false
            } catch (e: InterruptedException) { }
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
    private fun isOfflineMode(): Boolean {
        val prefs = context.getSharedPreferences("game", Context.MODE_PRIVATE)
        return prefs.getBoolean("offline_mode", false)
    }
    fun endGameAndReturnToMenu() {
        // Đảm bảo chỉ lưu phần coin vừa kiếm được
        val coinEarned = localCoin - coinBeforePlay
        if (!isOfflineMode()) {
            Firebase.auth.currentUser?.uid?.let { uid ->
                userRepo.addCoins(uid, coinEarned.toLong(),
                    onOk = { /* thông báo nếu muốn */ },
                    onErr = { msg -> android.widget.Toast.makeText(context, "Lỗi lưu coin: $msg", android.widget.Toast.LENGTH_LONG).show() }
                )
            }
        } else {
            val prefs = context.getSharedPreferences("game", Context.MODE_PRIVATE)
            prefs.edit().putLong("coins", localCoin.toLong()).apply()
        }
        // Chuyển về StartMenuActivity như cũ
        val activity = context as? android.app.Activity
        if (activity != null) {
            val intent = android.content.Intent(activity, StartMenuActivity::class.java)
            activity.startActivity(intent)
            activity.finish()
        }
    }

    fun update() {
        if (isLevelChanging) {
            levelChangeCounter++
            if (levelChangeCounter >= levelChangeDuration) {
                isLevelChanging = false
            }
            return
        }
        if (currentLevel.getLives() <= 0) {
            endGameAndReturnToMenu()
            return
        }

// Khi thắng tất cả level
        if (level > maxLevel) {
            endGameAndReturnToMenu()
            return
        }
        if (currentLevel.getLives() <= 0) return

        // Bắn tự động
        val interval = if (gunMode == GunMode.FAST) autoShootIntervalFast else autoShootIntervalNormal
        autoShootCounter++
        if (autoShootCounter >= interval) {
            val bulletY = player.y
            val center = player.x + playerBitmap.width / 2 - bulletBitmap.width / 2
            val offset = 40
            when (gunMode) {
                GunMode.NORMAL, GunMode.FAST -> {
                    bullets.add(Bullet(center, bulletY, bulletBitmap, 30, 2, 90.0))
                    soundPool.play(gunshotSoundId, 1f, 1f, 1, 0, 1f)
                }
                GunMode.TRIPLE_PARALLEL -> {
                    bullets.add(Bullet(center, bulletY, bulletBitmap, 30, 2, 90.0))
                    bullets.add(Bullet(center - offset, bulletY, bulletBitmap, 30, 2, 90.0))
                    bullets.add(Bullet(center + offset, bulletY, bulletBitmap, 30, 2, 90.0))
                    soundPool.play(gunshotSoundId, 1f, 1f, 1, 0, 1f)
                }
                GunMode.TRIPLE_SPREAD -> {
                    bullets.add(Bullet(center, bulletY, bulletBitmap, 30, 2, 90.0))
                    bullets.add(Bullet(center, bulletY, bulletBitmap, 30, 2, 110.0))
                    bullets.add(Bullet(center, bulletY, bulletBitmap, 30, 2, 70.0))
                    soundPool.play(gunshotSoundId, 1f, 1f, 1, 0, 1f)
                }
            }
            autoShootCounter = 0
        }

        bullets.forEach { it.update() }
        bullets.removeAll { it.y < 0 || it.x < 0 || it.x > width }

        currentLevel.update(bullets)

        // Item -> đổi súng
        currentLevel.pickedGunMode?.let {
            gunMode = it
            gunModeTimer = 0
            currentLevel.pickedGunMode = null
        }

        if (gunMode != GunMode.NORMAL) {
            gunModeTimer++
            if (gunModeTimer > gunModeDuration) gunMode = GunMode.NORMAL
        }

        if (currentLevel.isCompleted()) {
            if (level < maxLevel) {
                startLevel(level + 1)
            } else {
                isLevelChanging = true
            }
        }
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        currentLevel.draw(canvas, bullets)

        val paint = Paint()
        val lifeIcon = Bitmap.createScaledBitmap(playerBitmap, 60, 60, true)
        for (i in 0 until currentLevel.getLives()) {
            canvas.drawBitmap(lifeIcon, 40f + i * 70, 80f, paint)
        }

        // HUD hiển thị số xu
        paint.color = Color.YELLOW
        paint.textSize = 48f
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("Xu: $localCoin", width - 32f, 80f, paint)

        if (isLevelChanging) {
            paint.textSize = 80f
            paint.color = Color.YELLOW
            paint.textAlign = Paint.Align.CENTER
            val msg = when {
                currentLevel.getLives() <= 0 -> "GAME OVER"
                level > maxLevel -> "CHÚC MỪNG BẠN!"
                else -> "Level $level"
            }
            canvas.drawText(msg, width / 2f, height / 2f, paint)
            paint.textAlign = Paint.Align.LEFT
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_MOVE) {
            val newX = event.x.toInt() - playerBitmap.width / 2
            player.x = newX.coerceIn(0, width - playerBitmap.width)
        }
        return true
    }

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
                sleep(16)
            }
        }
    }
}
