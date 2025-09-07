package com.example.chickenshooter

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color
import android.graphics.Rect
import android.graphics.Path
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

    private var isGameOver = false
    private var gameOverCounter = 0
    private val gameOverWait = 90
    private var isReturningToMenu = false

    private var showGameOverMenu = false
    private var retryButtonRect: Rect? = null
    private var menuButtonRect: Rect? = null

    private var isPaused = false
    private var pauseButtonRect: Rect? = null
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
                        coinBeforePlay = coins.toInt()
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

        currentLevel.onCoinCollected = { amount: Int ->
            localCoin += amount
            // Lưu xu ngay khi nhặt được
            if (isOfflineMode()) {
                val prefs = context.getSharedPreferences("game", Context.MODE_PRIVATE)
                prefs.edit().putLong("coins", localCoin.toLong()).apply()
            } else {
                Firebase.auth.currentUser?.uid?.let { uid ->
                    userRepo.addCoins(uid, amount.toLong())
                }
            }
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
        val coinEarned = localCoin - coinBeforePlay
        if (!isOfflineMode()) {
            Firebase.auth.currentUser?.uid?.let { uid ->
                userRepo.addCoins(uid, coinEarned.toLong(),
                    onOk = { /* ... */ },
                    onErr = { msg -> android.widget.Toast.makeText(context, "Lỗi lưu coin: $msg", android.widget.Toast.LENGTH_LONG).show() }
                )
            }
        } else {
            val prefs = context.getSharedPreferences("game", Context.MODE_PRIVATE)
            prefs.edit().putLong("coins", localCoin.toLong()).apply()
        }
        thread.running = false
        post {
            val activity = context as? android.app.Activity
            if (activity != null) {
                val intent = android.content.Intent(activity, StartMenuActivity::class.java)
                activity.startActivity(intent)
                activity.finish()
            }
        }
    }

    fun update() {
        if (isPaused) return
        if (isLevelChanging) {
            levelChangeCounter++
            if (levelChangeCounter >= levelChangeDuration) {
                isLevelChanging = false
            }
            return
        }
        if (currentLevel.getLives() <= 0) {
            isGameOver = true
            showGameOverMenu = true
            return
        }
        if (isGameOver) {
            gameOverCounter++
            if (gameOverCounter > gameOverWait) {
                endGameAndReturnToMenu()
            }
            return
        }
        if (currentLevel.isCompleted()) {
            if (level < maxLevel) {
                startLevel(level + 1)
            } else {
                isLevelChanging = true
            }
            return
        }
        if (level > maxLevel) {
            endGameAndReturnToMenu()
            return
        }
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

        currentLevel.pickedGunMode?.let {
            gunMode = it
            gunModeTimer = 0
            currentLevel.pickedGunMode = null
        }

        if (gunMode != GunMode.NORMAL) {
            gunModeTimer++
            if (gunModeTimer > gunModeDuration) gunMode = GunMode.NORMAL
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

        // Vẽ số xu
        paint.color = Color.YELLOW
        paint.textSize = 48f
        paint.textAlign = Paint.Align.RIGHT
        val coinTextX = width - 32f
        val coinTextY = 80f
        canvas.drawText("Xu: $localCoin", coinTextX, coinTextY, paint)

        // Vẽ nút pause/play bên trái chữ xu, đảm bảo không đè lên
        val pauseBtnSize = 48f
        val pauseBtnX = coinTextX - pauseBtnSize - 150f // Di chuyển nút sang trái xa hơn chữ xu
        val pauseBtnY = coinTextY - pauseBtnSize + 8f  // Di chuyển nút lên trên một chút
        pauseButtonRect = Rect(
            pauseBtnX.toInt(),
            pauseBtnY.toInt(),
            (pauseBtnX + pauseBtnSize).toInt(),
            (pauseBtnY + pauseBtnSize).toInt()
        )

        paint.color = Color.WHITE
        paint.strokeWidth = 8f
        val centerX = pauseBtnX + pauseBtnSize / 2
        val centerY = pauseBtnY + pauseBtnSize / 2

        if (!isPaused) {
            // Đang chơi: vẽ icon PAUSE ||
            val barW = 8f
            val barH = pauseBtnSize - 16f
            val gap = 10f
            canvas.drawRect(
                centerX - barW - gap / 2, centerY - barH / 2,
                centerX - gap / 2, centerY + barH / 2,
                paint
            )
            canvas.drawRect(
                centerX + gap / 2, centerY - barH / 2,
                centerX + barW + gap / 2, centerY + barH / 2,
                paint
            )
        } else {
            // Đang PAUSE: vẽ tam giác ngang ▶
            val triangleSize = pauseBtnSize * 0.6f
            val path = Path()
            path.moveTo(centerX - triangleSize / 2, centerY - triangleSize / 2)
            path.lineTo(centerX - triangleSize / 2, centerY + triangleSize / 2)
            path.lineTo(centerX + triangleSize / 2, centerY)
            path.close()
            canvas.drawPath(path, paint)
        }

        paint.textAlign = Paint.Align.LEFT

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
        if (isGameOver && showGameOverMenu) {
            paint.textSize = 80f
            paint.color = Color.RED
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText("GAME OVER", width / 2f, height / 2f - 100, paint)

            val btnWidth = 350
            val btnHeight = 100
            val btnSpacing = 60
            val centerXBtn = width / 2
            val retryY = height / 2 + 20
            val menuY = retryY + btnHeight + btnSpacing

            retryButtonRect = Rect(centerXBtn - btnWidth/2, retryY, centerXBtn + btnWidth/2, retryY + btnHeight)
            paint.color = Color.parseColor("#4CAF50")
            canvas.drawRect(retryButtonRect!!, paint)
            paint.color = Color.WHITE
            paint.textSize = 52f
            canvas.drawText("Chơi lại", centerXBtn.toFloat(), retryY + btnHeight/2f + 18f, paint)

            menuButtonRect = Rect(centerXBtn - btnWidth/2, menuY, centerXBtn + btnWidth/2, menuY + btnHeight)
            paint.color = Color.parseColor("#2196F3")
            canvas.drawRect(menuButtonRect!!, paint)
            paint.color = Color.WHITE
            canvas.drawText("Menu", centerXBtn.toFloat(), menuY + btnHeight/2f + 18f, paint)

            paint.textAlign = Paint.Align.LEFT
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Xử lý bấm nút Pause/Play
        if (pauseButtonRect != null && event.action == MotionEvent.ACTION_DOWN) {
            val x = event.x.toInt()
            val y = event.y.toInt()
            if (pauseButtonRect!!.contains(x, y)) {
                isPaused = !isPaused
                return true // KHÔNG xử lý di chuyển máy bay khi ấn vào nút
            }
        }
        // Nếu đang pause thì không xử lý gì thêm
        if (isPaused) return true
        // Xử lý menu Game Over
        if (isGameOver && showGameOverMenu) {
            if (event.action == MotionEvent.ACTION_DOWN) {
                val x = event.x.toInt()
                val y = event.y.toInt()
                if (retryButtonRect?.contains(x, y) == true) {
                    showGameOverMenu = false
                    isGameOver = false
                    currentLevel.reset()
                    bullets.clear()
                    gunMode = GunMode.NORMAL
                    gunModeTimer = 0
                    autoShootCounter = 0
                    return true
                }
                if (menuButtonRect?.contains(x, y) == true) {
                    endGameAndReturnToMenu()
                    return true
                }
            }
            return true
        }
        if (isGameOver) return false
        // Chỉ xử lý di chuyển máy bay khi không nhấn vào nút pause và không pause
        if ((event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_MOVE)
            && (pauseButtonRect == null || !pauseButtonRect!!.contains(event.x.toInt(), event.y.toInt()))
        ) {
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