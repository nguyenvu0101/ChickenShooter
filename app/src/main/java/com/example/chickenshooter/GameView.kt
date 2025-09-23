package com.example.chickenshooter

    import android.content.Context
    import android.graphics.BitmapFactory
    import android.graphics.Bitmap
    import android.graphics.Canvas
    import android.graphics.Paint
    import android.graphics.Color
    import android.graphics.Rect
    import android.graphics.RectF
    import android.graphics.Path
    import android.view.MotionEvent
    import android.view.SurfaceHolder
    import android.view.SurfaceView
    import android.media.SoundPool
    import com.example.chickenshooter.levels.*
    import com.google.firebase.auth.ktx.auth
    import com.google.firebase.ktx.Firebase
    import android.media.MediaPlayer
    // CHế độ bắn súng
    enum class GunMode {
        NORMAL, FAST, TRIPLE_PARALLEL, TRIPLE_SPREAD
    }

    class GameView(context: Context, private val backgroundId: Int, planeId: Int) :
        SurfaceView(context), SurfaceHolder.Callback {

        private val thread: GameThread

        // Obj máy bay
        private val playerBitmap = BitmapFactory.decodeResource(resources, planeId)

        // obj đạn
        private val bulletBitmap = BitmapFactory.decodeResource(resources, R.drawable.bullet)

        // obj nút tên lửa
        private val missileButtonBitmap =
            BitmapFactory.decodeResource(context.resources, R.drawable.rocket1)

        // obj xu
        private val coinBitmap = BitmapFactory.decodeResource(resources, R.drawable.coin)

        // scale lại tên lửa cho nó nhỏ lại
        private val missileButtonBitmapScaled = Bitmap.createScaledBitmap(
            missileButtonBitmap,
            (playerBitmap.width * 0.6).toInt(),
            (playerBitmap.height * 0.6).toInt(),
            true
        )
        private var mediaPlayer: MediaPlayer? = null
        // obj tên lửa bắn từ máy bay
        private val missileBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.rocket1)

        // obj mô tả hiệu ứng nổ tên lửa
        private val missileExplosionBitmap =
            BitmapFactory.decodeResource(context.resources, R.drawable.explosion_missile)

        // biến quản lí tên lửa
        private var missile: Missile? = null
        private var missileBtnRect: Rect? = null

        val shields = mutableListOf<Shield>()
        // item rơi từ con gà
        private val itemBitmaps = listOf(
            BitmapFactory.decodeResource(resources, R.drawable.item_fast),
            BitmapFactory.decodeResource(resources, R.drawable.item_parallel),
            BitmapFactory.decodeResource(resources, R.drawable.item_spread)
        )

        private lateinit var player: Player
        private val bullets = mutableListOf<Bullet>()

        // Movement variables for 4-directional smooth movement
        private var targetX = 0f
        private var targetY = 0f
        private val movementSpeed = 45f // Increased from 8f to 12f for faster movement
        private var isMoving = false

        // Sound effect variables
        private var borderSoundId: Int = 0

        // âm thanh
        private lateinit var soundPool: SoundPool
        private var gunshotSoundId: Int = 0
        private var missileSoundId: Int = 0
        private var warningSoundId: Int = 0

        // Thêm biến trạng thái
        private var showPauseMenu = false
        private var isMusicOn = true
        private var isSoundOn = true

        // Rect cho các nút trong menu pause
        private var pauseExitButtonRect: Rect? = null
        private var pauseMusicButtonRect: Rect? = null
        private var pauseSoundButtonRect: Rect? = null

        private val levelMusicResIds = listOf(
            R.raw.music_level1,
            R.raw.music_level2,
            R.raw.music_level3,
            R.raw.music_level4
        )
        // Bitmap cho các icon bật/tắt nhạc nền và âm thanh
        private val musicOnBitmap = BitmapFactory.decodeResource(resources, R.drawable.music_turnon)
        private val musicOffBitmap = BitmapFactory.decodeResource(resources, R.drawable.music_turnoff)
        private val soundOnBitmap = BitmapFactory.decodeResource(resources, R.drawable.sound_on)
        private val soundOffBitmap = BitmapFactory.decodeResource(resources, R.drawable.sound_off)
        // Kích thước icon (ví dụ 48 hoặc 64 tùy nút)
        private val pauseIconSize = 64

        private val musicOnBitmapScaled = Bitmap.createScaledBitmap(musicOnBitmap, pauseIconSize, pauseIconSize, true)
        private val musicOffBitmapScaled = Bitmap.createScaledBitmap(musicOffBitmap, pauseIconSize, pauseIconSize, true)
        private val soundOnBitmapScaled = Bitmap.createScaledBitmap(soundOnBitmap, pauseIconSize, pauseIconSize, true)
        private val soundOffBitmapScaled = Bitmap.createScaledBitmap(soundOffBitmap, pauseIconSize, pauseIconSize, true)
        // Warning system for halfway screen crossing
        private var hasPlayedWarning = false
        private var warningCooldown = 0
        private val warningCooldownDuration = 90 // 1.5 seconds at 60fps (reduced from 180)

        // Level
        private var level = 1
        private val maxLevel = 4
        private lateinit var currentLevel: BaseLevel

        // Chế độ bắn
        private var gunMode = GunMode.NORMAL
        private var gunModeTimer = 0
        private val gunModeDuration = 8 * 60
        private var autoShootCounter = 0
        private val autoShootIntervalNormal = 10
        private val autoShootIntervalFast = 5

        // Cấu hình chuyển level
        private var isLevelChanging = false
        private var levelChangeCounter = 0
        private val levelChangeDuration = 90

        // game over
        private var isGameOver = false
        private var gameOverCounter = 0
        private val gameOverWait = 90
        private var isReturningToMenu = false

        // menu game
        private var showGameOverMenu = false
        private var retryButtonRect: Rect? = null
        private var menuButtonRect: Rect? = null

        // Pause
        private var isPaused = false
        private var pauseButtonRect: Rect? = null

        // Coins
        private val userRepo = UserRepoRTDB()
        private var localCoin = 0
        private var coinBeforePlay = 0

        init {
            holder.addCallback(this)
            thread = GameThread(holder, this)

            // Khởi tạo bitmap cho projectiles
            ChickenProjectile.init(context.resources)

            isFocusable = true
        }

        override fun surfaceDestroyed(holder: SurfaceHolder) {
            var retry = true
            thread.running = false
            soundPool.release()
            while (retry) {
                try {
                    thread.join()
                    retry = false
                } catch (e: InterruptedException) {
                }
            }
        }

        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

        override fun surfaceCreated(holder: SurfaceHolder) {
            // tạo tọa độ giữa màn hình cách 30px so với bottom
            val centerX = width / 2 - playerBitmap.width / 2
            val bottomY = height - playerBitmap.height - 30

            // khởi tạo obj máy bay
            player = Player(centerX, bottomY, playerBitmap)

            soundPool = SoundPool.Builder().setMaxStreams(5).build()
            gunshotSoundId = soundPool.load(context, R.raw.laser, 1)
            missileSoundId = soundPool.load(context, R.raw.tieng_bom_no, 1)
            borderSoundId = soundPool.load(context, R.raw.border, 1)
            warningSoundId = soundPool.load(context, R.raw.warning, 1)

            // Initialize target positions for smooth movement
            targetX = centerX.toFloat()
            targetY = bottomY.toFloat()


            // chức năng online/offline
            if (isOfflineMode()) {
                val prefs = context.getSharedPreferences("game", Context.MODE_PRIVATE)
                localCoin = prefs.getLong("coins", 0L).toInt()
                coinBeforePlay = localCoin
                startLevel(1)
                thread.running = true
                thread.start()
            } else {
                Firebase.auth.currentUser?.uid?.let { uid ->
                    userRepo.loadOnlineProfileOnce(
                        uid,
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

            // 1. Giải phóng nhạc nền cũ nếu có (tránh trùng tiếng, tránh leak RAM)
            mediaPlayer?.release()
            mediaPlayer = null

            // 2. Gán nhạc nền cho từng màn, chỉ play khi isMusicOn = true
            if (level in 1..levelMusicResIds.size) {
                mediaPlayer = MediaPlayer.create(context, levelMusicResIds[level - 1])
                mediaPlayer?.isLooping = true
                if (isMusicOn) mediaPlayer?.start()
            }

            // 3. Khởi tạo màn chơi theo level như cũ
            currentLevel = when (level) {
                1 -> Level1(context, player, bulletBitmap, itemBitmaps, coinBitmap, backgroundId)
                2 -> Level2(context, player, bulletBitmap, itemBitmaps, coinBitmap, backgroundId)
                3 -> Level3(context, player, bulletBitmap, itemBitmaps, coinBitmap, backgroundId)
                4 -> Level4(context, player, bulletBitmap, itemBitmaps, coinBitmap, backgroundId)
                else -> Level1(context, player, bulletBitmap, itemBitmaps, coinBitmap, backgroundId)
            }
            currentLevel.setScreenSize(width, height)
// Đặt lại vị trí player
            player.x = width / 2 - playerBitmap.width / 2
            player.y = height - playerBitmap.height - 30
            targetX = player.x.toFloat()
            targetY = player.y.toFloat()
            isMoving = false
            // 4. Xử lý cộng xu khi nhặt coin
            currentLevel.onCoinCollected = { amount: Int ->
                localCoin += amount
                if (isOfflineMode()) {
                    val prefs = context.getSharedPreferences("game", Context.MODE_PRIVATE)
                    prefs.edit().putLong("coins", localCoin.toLong()).apply()
                } else {
                    Firebase.auth.currentUser?.uid?.let { uid ->
                        userRepo.addCoins(uid, amount.toLong())
                    }
                }
            }

            // 5. Reset các biến hỗ trợ level
            currentLevel.reset()
            bullets.clear()
            gunMode = GunMode.NORMAL
            gunModeTimer = 0
            autoShootCounter = 0
            isLevelChanging = true
            levelChangeCounter = 0
        }

        //
        private fun isOfflineMode(): Boolean {
            val prefs = context.getSharedPreferences("game", Context.MODE_PRIVATE)
            return prefs.getBoolean("offline_mode", false)
        }

        fun endGameAndReturnToMenu() {
            val coinEarned = localCoin - coinBeforePlay
            if (!isOfflineMode()) {
                Firebase.auth.currentUser?.uid?.let { uid ->
                    userRepo.addCoins(
                        uid, coinEarned.toLong(),
                        onOk = { /* ... */ },
                        onErr = { msg ->
                            android.widget.Toast.makeText(
                                context,
                                "Lỗi lưu coin: $msg",
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                        }
                    )
                }
            } else {
                val prefs = context.getSharedPreferences("game", Context.MODE_PRIVATE)
                prefs.edit().putLong("coins", localCoin.toLong()).apply()
            }
            mediaPlayer?.release()
            mediaPlayer = null
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

            // delay chuyênr level
            if (isLevelChanging) {
                levelChangeCounter++
                if (levelChangeCounter >= levelChangeDuration) {
                    isLevelChanging = false
                }
                return
            }

            // nếu hết mạng thì mở game over
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
            // nếu end màn thì mở màn mới hoặc end game
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

            val interval =
                if (gunMode == GunMode.FAST) autoShootIntervalFast else autoShootIntervalNormal
            autoShootCounter++

            if (autoShootCounter >= interval) {
                val bulletY = player.y
                val center = player.x + playerBitmap.width / 2 - bulletBitmap.width / 2
                val offset = 40
                when (gunMode) {
                    GunMode.NORMAL, GunMode.FAST -> {
                        bullets.add(Bullet(center, bulletY, bulletBitmap, 30, 2, 90.0))
                        if (isSoundOn) soundPool.play(gunshotSoundId, 1f, 1f, 1, 0, 1f)
                    }

                    GunMode.TRIPLE_PARALLEL -> {
                        bullets.add(Bullet(center, bulletY, bulletBitmap, 30, 2, 90.0))
                        bullets.add(Bullet(center - offset, bulletY, bulletBitmap, 30, 2, 90.0))
                        bullets.add(Bullet(center + offset, bulletY, bulletBitmap, 30, 2, 90.0))

                        if (isSoundOn) soundPool.play(gunshotSoundId, 1f, 1f, 1, 0, 1f)
                    }

                    GunMode.TRIPLE_SPREAD -> {
                        bullets.add(Bullet(center, bulletY, bulletBitmap, 30, 2, 90.0))
                        bullets.add(Bullet(center, bulletY, bulletBitmap, 30, 2, 110.0))
                        bullets.add(Bullet(center, bulletY, bulletBitmap, 30, 2, 70.0))
                        if (isSoundOn) soundPool.play(gunshotSoundId, 1f, 1f, 1, 0, 1f)
                    }
                }
                autoShootCounter = 0
            }

            // update đạn, xóa khoỉ màn hình
            bullets.forEach { it.update() }
            bullets.removeAll { it.y < 0 || it.x < 0 || it.x > width }

            // update level
            currentLevel.update(bullets)

            currentLevel.pickedGunMode?.let {
                gunMode = it
                gunModeTimer = 0
                currentLevel.pickedGunMode = null
            }
            // item đổi súng
            if (gunMode != GunMode.NORMAL) {
                gunModeTimer++
                if (gunModeTimer > gunModeDuration) gunMode = GunMode.NORMAL
            }
            missile?.update()
// Khi tên lửa vừa bắt đầu nổ, xóa sạch quái và trừ máu boss cho mọi level
            if (missile != null && missile!!.isExploding && missile!!.explosionFrame == 1) {
                when (currentLevel) {
                    is Level1 -> {
                        val level = currentLevel as Level1
                        level.chickens.clear()
                        level.boss?.let { boss ->
                            boss.hp -= 50 // trừ 50 máu, có thể chỉnh tùy ý
                            if (boss.hp < 0) boss.hp = 0
                        }
                    }
                    is Level2 -> {
                        val level = currentLevel as Level2
                        level.chickens.clear()
                        level.boss?.let { boss ->
                            boss.hp -= 50
                            if (boss.hp < 0) boss.hp = 0
                        }
                    }
                    is Level3 -> {
                        val level = currentLevel as Level3
                        level.chickens.clear()
                        level.boss?.let { boss ->
                            boss.hp -= 50
                            if (boss.hp < 0) boss.hp = 0
                        }
                    }
                    is Level4 -> {
                        val level = currentLevel as Level4
                        level.chickens.clear()
                        level.boss?.let { boss ->
                            boss.hp -= 50
                            if (boss.hp < 0) boss.hp = 0
                        }
                    }
                }
            }
            // Xóa hiệu ứng khi nổ xong
            if (missile?.isFinished() == true) {
                missile = null
                currentLevel.manaCount = 0
            }

            // Xử lý di chuyển mượt mà cho máy bay
            if (isMoving) {
                val dx = targetX - player.x
                val dy = targetY - player.y
                val distance = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()

                // Nếu khoảng cách còn lại lớn hơn tốc độ di chuyển, tiếp tục di chuyển
                if (distance > movementSpeed) {
                    player.x = (player.x + (dx / distance * movementSpeed)).toInt()
                    player.y = (player.y + (dy / distance * movementSpeed)).toInt()
                } else {
                    // Đến gần mục tiêu, đặt lại vị trí chính xác
                    player.x = targetX.toInt()
                    player.y = targetY.toInt()
                    isMoving = false
                }
            }

            // Kiểm tra va chạm với biên màn hình và phát âm thanh (trừ biên dưới)
            val borderThreshold = 5f // ngưỡng để tránh phát âm liên tục
            if (player.x <= borderThreshold || player.x >= width - playerBitmap.width - borderThreshold ||
                player.y <= borderThreshold) { // Removed bottom border check
                if (isSoundOn) soundPool.play(gunshotSoundId, 1f, 1f, 1, 0, 1f)
            }

            // Giới hạn player trong màn hình
            player.x = player.x.coerceIn(0, width - playerBitmap.width)
            player.y = player.y.coerceIn(0, height - playerBitmap.height)

            // Kiểm tra cảnh báo khi máy bay vượt qua 1/2 màn hình
            val halfScreenY = height / 2
            val playerCenterY = player.y + playerBitmap.height / 2

            // Quản lý cooldown cho warning
            if (warningCooldown > 0) {
                warningCooldown--
            }

            // Kiểm tra nếu máy bay vượt qua 1/2 màn hình và chưa phát warning gần đây
            if (playerCenterY <= halfScreenY && !hasPlayedWarning && warningCooldown <= 0) {
                if (isSoundOn) soundPool.play(warningSoundId, 0.7f, 0.7f, 1, 0, 1f)
                hasPlayedWarning = true
                warningCooldown = warningCooldownDuration
            }

            // Reset warning flag khi máy bay trở lại nửa dưới màn hình
            if (playerCenterY > halfScreenY) {
                hasPlayedWarning = false
            }
        }

        override fun draw(canvas: Canvas) {
            super.draw(canvas)
            currentLevel.draw(canvas, bullets)

            val paint = Paint()
            val currentMana = currentLevel.manaCount
            val manaNeeded = currentLevel.manaNeededForMissile
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

            // Vẽ nút pause/play bên trái chữ xu
            val pauseBtnSize = 48f
            val pauseBtnX = coinTextX - pauseBtnSize - 170f
            val pauseBtnY = coinTextY - pauseBtnSize + 8f
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

            // Vẽ nút tên lửa/missile
            val btnX = width - missileButtonBitmapScaled.width - 40
            val btnY = height - missileButtonBitmapScaled.height - 40
            canvas.drawBitmap(missileButtonBitmapScaled, btnX.toFloat(), btnY.toFloat(), null)

            // Thông số vòng tròn mana cho tên lửa
            val cx = btnX + missileButtonBitmapScaled.width / 2f
            val cy = btnY + missileButtonBitmapScaled.height / 2f
            val radius = missileButtonBitmapScaled.width / 2f + 16f
            val manaStrokeWidth = 14f
            val sweep = 360f / manaNeeded

            val paintFilled = Paint().apply {
                color = Color.CYAN
                style = Paint.Style.STROKE
                strokeWidth = manaStrokeWidth
                isAntiAlias = true
            }
            val paintEmpty = Paint().apply {
                color = Color.argb(80, 200, 200, 200)
                style = Paint.Style.STROKE
                strokeWidth = manaStrokeWidth
                isAntiAlias = true
            }

            // Vẽ các cung đã nhặt được mana
            for (i in 0 until currentMana.coerceAtMost(manaNeeded)) {
                val startAngle = -90f + i * sweep
                canvas.drawArc(
                    cx - radius, cy - radius, cx + radius, cy + radius,
                    startAngle, sweep - 6,  // trừ 6 độ để có khe
                    false, paintFilled
                )
            }
            // Vẽ các cung chưa nhặt
            for (i in currentMana until manaNeeded) {
                val startAngle = -90f + i * sweep
                canvas.drawArc(
                    cx - radius, cy - radius, cx + radius, cy + radius,
                    startAngle, sweep - 6,
                    false, paintEmpty
                )
            }
            missileBtnRect = Rect(
                btnX, btnY,
                btnX + missileButtonBitmapScaled.width,
                btnY + missileButtonBitmapScaled.height
            )

            // Vẽ nút pause/play
            if (!isPaused) {
                // Đang chơi: vẽ icon PAUSE
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
                // Đang PAUSE: vẽ tam giác ngang (icon play)
                val triangleSize = pauseBtnSize * 0.6f
                val path = Path()
                path.moveTo(centerX - triangleSize / 2, centerY - triangleSize / 2)
                path.lineTo(centerX - triangleSize / 2, centerY + triangleSize / 2)
                path.lineTo(centerX + triangleSize / 2, centerY)
                path.close()
                canvas.drawPath(path, paint)
            }

            // MENU PAUSE
            if (showPauseMenu) {
                val menuWidth = width * 2 / 3
                val menuHeight = 500
                val left = (width - menuWidth) / 2
                val top = (height - menuHeight) / 2

                // Nền menu
                paint.color = Color.argb(220, 30, 30, 30)
                canvas.drawRoundRect(left.toFloat(), top.toFloat(), (left+menuWidth).toFloat(), (top+menuHeight).toFloat(), 40f, 40f, paint)

                // Vẽ nút EXIT
                val btnW = menuWidth - 80
                val btnH = 110
                val btnX = left + 40
                var btnY = top + 40
                pauseExitButtonRect = Rect(btnX, btnY, btnX+btnW, btnY+btnH)
                paint.color = Color.parseColor("#F44336")
                canvas.drawRoundRect(RectF(pauseExitButtonRect), 26f,26f, paint)
                paint.color = Color.WHITE
                paint.textSize = 52f
                paint.textAlign = Paint.Align.CENTER
                canvas.drawText("Thoát game", (btnX+btnW/2).toFloat(), (btnY+btnH/2)+18f, paint)

                // Nút bật/tắt nhạc nền
                btnY += btnH + 25
                pauseMusicButtonRect = Rect(btnX, btnY, btnX+btnW, btnY+btnH)
                paint.color = Color.parseColor("#009688")
                canvas.drawRoundRect(RectF(pauseMusicButtonRect), 26f,26f, paint)
                val musicIcon = if (isMusicOn) musicOnBitmapScaled else musicOffBitmapScaled
                val musicX = btnX + 24f
                val musicY = btnY + (btnH - pauseIconSize) / 2f
                canvas.drawBitmap(musicIcon, musicX, musicY, null)
                paint.color = Color.WHITE
                paint.textAlign = Paint.Align.LEFT
                canvas.drawText(
                    if (isMusicOn) "Tắt nhạc nền" else "Bật nhạc nền",
                    (btnX+120).toFloat(), (btnY+btnH/2)+18f, paint
                )

                // Nút bật/tắt hiệu ứng âm thanh
                btnY += btnH + 25
                pauseSoundButtonRect = Rect(btnX, btnY, btnX+btnW, btnY+btnH)
                paint.color = Color.parseColor("#3F51B5")
                canvas.drawRoundRect(RectF(pauseSoundButtonRect), 26f,26f, paint)
                val soundIcon = if (isSoundOn) soundOnBitmapScaled else soundOffBitmapScaled
                val soundX = btnX + 24f
                val soundY = btnY + (btnH - pauseIconSize) / 2f
                canvas.drawBitmap(soundIcon, soundX, soundY, null)
                paint.color = Color.WHITE
                paint.textAlign = Paint.Align.LEFT
                canvas.drawText(
                    if (isSoundOn) "Tắt hiệu ứng" else "Bật hiệu ứng",
                    (btnX+120).toFloat(), (btnY+btnH/2)+18f, paint
                )
                paint.textAlign = Paint.Align.LEFT
                return
            }

            // Vẽ tên lửa nếu có
            missile?.draw(canvas, missileExplosionBitmap)
            paint.textAlign = Paint.Align.LEFT

            // Vẽ hiệu ứng chuyển màn, game over, menu game over
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

                retryButtonRect = Rect(
                    centerXBtn - btnWidth / 2,
                    retryY,
                    centerXBtn + btnWidth / 2,
                    retryY + btnHeight
                )
                paint.color = Color.parseColor("#4CAF50")
                canvas.drawRect(retryButtonRect!!, paint)
                paint.color = Color.WHITE
                paint.textSize = 52f
                canvas.drawText("Chơi lại", centerXBtn.toFloat(), retryY + btnHeight / 2f + 18f, paint)

                menuButtonRect =
                    Rect(centerXBtn - btnWidth / 2, menuY, centerXBtn + btnWidth / 2, menuY + btnHeight)
                paint.color = Color.parseColor("#2196F3")
                canvas.drawRect(menuButtonRect!!, paint)
                paint.color = Color.WHITE
                canvas.drawText("Menu", centerXBtn.toFloat(), menuY + btnHeight / 2f + 18f, paint)

                paint.textAlign = Paint.Align.LEFT
            }
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            // Xử lý bấm nút Pause/Play
            // Nếu đang show menu pause, chỉ xử lý các nút này
            if (showPauseMenu) {
                if (event.action == MotionEvent.ACTION_DOWN) {
                    val x = event.x.toInt()
                    val y = event.y.toInt()
                    if (pauseExitButtonRect?.contains(x, y) == true) {
                        endGameAndReturnToMenu()
                        return true
                    }
                    if (pauseMusicButtonRect?.contains(x, y) == true) {
                        isMusicOn = !isMusicOn
                        if (isMusicOn) mediaPlayer?.start() else mediaPlayer?.pause()
                        return true
                    }
                    if (pauseSoundButtonRect?.contains(x, y) == true) {
                        isSoundOn = !isSoundOn
                        return true
                    }
                    // *** Sửa tại đây: kiểm tra pauseButtonRect ***
                    if (pauseButtonRect != null && pauseButtonRect!!.contains(x, y)) {
                        isPaused = false
                        showPauseMenu = false
                        if (isMusicOn) mediaPlayer?.start()
                        return true
                    }
                }
                return true
            }

            // Xử lý nút pause như cũ, nhưng mở menu thay vì chỉ toggle play/pause
            if (pauseButtonRect != null && event.action == MotionEvent.ACTION_DOWN) {
                val x = event.x.toInt()
                val y = event.y.toInt()
                if (pauseButtonRect!!.contains(x, y)) {
                    isPaused = !isPaused
                    showPauseMenu = isPaused
                    if (isPaused) mediaPlayer?.pause() else if (isMusicOn) mediaPlayer?.start()
                    return true
                }
            }
            if (missileBtnRect != null && event.action == MotionEvent.ACTION_DOWN) {
                val x = event.x.toInt()
                val y = event.y.toInt()
                if (missileBtnRect!!.contains(x, y)) {
                    // Đảm bảo chỉ kích hoạt nếu chưa có hiệu ứng và đủ mana
                    if (missile == null && currentLevel.canUseMissile()) {
                        currentLevel.consumeManaForMissile()
                        val targetX = (width - missileBitmap.width) / 2
                        val targetY = (height - missileBitmap.height) / 2
                        if (isSoundOn)  soundPool.play(missileSoundId, 1f, 1f, 1, 0, 1f)
                        missile = Missile(
                            player.x,
                            player.y,
                            missileButtonBitmapScaled, // dùng bitmap đã scale
                            (width - missileButtonBitmapScaled.width) / 2,
                            (height - missileButtonBitmapScaled.height) / 2
                        )
                    }
                    return true
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
                        isLevelChanging = false
                        levelChangeCounter = 0
                        gameOverCounter = 0

                        startLevel(level)

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

            // Xử lý di chuyển 4 hướng mượt mà khi không nhấn vào các nút đặc biệt
            if ((event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_MOVE)
                && (pauseButtonRect == null || !pauseButtonRect!!.contains(
                    event.x.toInt(),
                    event.y.toInt()
                ))
                && (missileBtnRect == null || !missileBtnRect!!.contains(
                    event.x.toInt(),
                    event.y.toInt()
                ))
            ) {
                // Thiết lập vị trí đích cho di chuyển mượt mà theo cả X và Y
                val newTargetX = (event.x - playerBitmap.width / 2).coerceIn(0f, (width - playerBitmap.width).toFloat())
                val newTargetY = (event.y - playerBitmap.height / 2).coerceIn(0f, (height - playerBitmap.height).toFloat())

                // Chỉ cập nhật target nếu có sự thay đổi đáng kể để tránh rung
                val threshold = 10f
                if (Math.abs(newTargetX - targetX) > threshold || Math.abs(newTargetY - targetY) > threshold) {
                    targetX = newTargetX
                    targetY = newTargetY
                    isMoving = true
                }
                return true
            }

            // Dừng di chuyển khi thả tay
            if (event.action == MotionEvent.ACTION_UP) {
                isMoving = false
            }

            return true
        }

        inner class GameThread(
            private val surfaceHolder: SurfaceHolder,
            private val gameView: GameView
        ) : Thread() {
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