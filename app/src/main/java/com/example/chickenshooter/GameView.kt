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
    import android.graphics.PointF
    import android.view.MotionEvent
    import android.view.SurfaceHolder
    import android.view.SurfaceView
    import android.media.SoundPool
    import com.example.chickenshooter.levels.*
    import android.media.MediaPlayer
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
    // CHế độ bắn súng
    enum class GunMode {
        NORMAL, FAST, TRIPLE_PARALLEL, TRIPLE_SPREAD
    }

    // Phase system cho story integration
    private enum class Phase { INTRO, PLAYING, RESCUE, DIALOG }

    // Story data structures
    data class LevelScript(val opening: DialogueScene, val ending: DialogueScene)

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
        private var localCoin = 0
        private var coinBeforePlay = 0

        // Memory management method
        private fun cleanupPreviousLevel() {
            try {
                android.util.Log.d("GameView", "Cleaning up previous level resources...")
                
                // Stop and release MediaPlayer
                mediaPlayer?.let {
                    if (it.isPlaying) it.stop()
                    it.release()
                    mediaPlayer = null
                    android.util.Log.d("GameView", "MediaPlayer released")
                }
                
                // Cleanup level if it exists
                if (::currentLevel.isInitialized) {
                    currentLevel.cleanup() // Assume levels have cleanup method
                    android.util.Log.d("GameView", "Level cleaned up")
                }
                
                // MEMORY LEAK FIX: Recycle cached bitmaps
                lifeIconBitmap?.let {
                    if (!it.isRecycled) {
                        it.recycle()
                    }
                    lifeIconBitmap = null
                    android.util.Log.d("GameView", "Life icon bitmap recycled")
                }
                
                // Force garbage collection to free memory immediately
                System.gc()
                
                // Short delay to allow GC to complete
                Thread.sleep(50)
                android.util.Log.d("GameView", "Cleanup completed, GC requested")
                
            } catch (e: Exception) {
                android.util.Log.e("GameView", "Error during cleanup: ${e.message}")
                e.printStackTrace()
            }
        }

        // Story managers và phase system
        private var phase = Phase.INTRO
        private lateinit var introManager: IntroManager
        private lateinit var dialogManager: DialogManager
        private var rescueAvatar: RescueAvatar? = null
        private var rescueTimeout = 0 // Timeout để tránh stuck ở rescue phase
        private var updateCallCount = 0 // Emergency counter để detect infinite loops
        private lateinit var avatarMap: Map<Speaker, Bitmap>
        
        // Input queue system for non-blocking touch handling
        private val inputQueue = InputQueue()
        private lateinit var dialoguePreloader: DialoguePreloader
        private val performanceMonitor = PerformanceMonitor()
        
        // MEMORY LEAK FIX: Cache life icon bitmap
        private var lifeIconBitmap: Bitmap? = null
        
        // MEMORY LEAK FIX: Cache Paint objects to avoid creating new ones every frame
        private val uiPaint = Paint()
        private val manaFilledPaint = Paint().apply {
            color = Color.CYAN
            style = Paint.Style.STROKE
            strokeWidth = 8f
            isAntiAlias = true
        }
        private val manaEmptyPaint = Paint().apply {
            color = Color.argb(80, 200, 200, 200)
            style = Paint.Style.STROKE
            strokeWidth = 8f
            isAntiAlias = true
        }
        
        // Flags để theo dõi story content
        private var isFirstGameStart = true // Chỉ hiện intro lần đầu
        private var shouldShowStoryContent = true // Hiện dialogue và rescue cho lần đầu chơi

        // Story content
        private val RESCUED_BY_LEVEL = mapOf(
            1 to Speaker.SOL,
            2 to Speaker.KICM,
            3 to Speaker.THIEN_AN,
            4 to Speaker.FIREFLIES
        )

       private val STORY = mapOf(
    1 to LevelScript(
        // Rescued: SOL
        opening = DialogueScene(listOf(
            DialogueLine(Speaker.JACK,      "Sol, bám vững nhé! Cha đang mở đường xuyên qua ổ gà thiên hà đây!"),
            DialogueLine(Speaker.SOL,       "Con thấy ánh đèn của cha rồi! Con ở đây, cha đừng lo!"),
            DialogueLine(Speaker.BOSS,      "Vô ích, phàm nhân! Ổ gà của ta không dành cho tình thân yếu đuối!")
        )),
        ending = DialogueScene(listOf(
            DialogueLine(Speaker.JACK,      "Cha đến rồi, Sol! Từ nay bất cứ nơi đâu con ở, cha cũng tìm thấy."),
            DialogueLine(Speaker.SOL,       "Con tin cha mà! Dải ngân hà rộng lớn nhưng trái tim con không còn sợ."),
            DialogueLine(Speaker.BOSS,      "Khặc… khặc… tận hưởng đi! Lần tới, lông cánh thép của ta sẽ nghiền nát hy vọng!")
        ))
    ),

    2 to LevelScript(
        // Rescued: KICM — UPDATED
        opening = DialogueScene(listOf(
            DialogueLine(Speaker.JACK,      "K-ICM! Mình đã mở hành lang lửa—bám theo quỹ đạo đạn của mình!"),
            DialogueLine(Speaker.KICM,      "Rõ! Mình thả vòng bass gây nhiễu tín hiệu—cậu cứ lao thẳng vào lõi phòng tuyến!"),
            DialogueLine(Speaker.BOSS,      "Nhạc của các ngươi chỉ là tạp âm trước tiếng gáy tối hậu của ta!")
        )),
        ending = DialogueScene(listOf(
            DialogueLine(Speaker.JACK,      "Xích đã đứt rồi. Cùng nhau viết lại giai điệu cho cả bầu trời này."),
            DialogueLine(Speaker.KICM,      "Đổi nhịp sang tự do—từ nay beat của chúng ta át mọi tiếng gà trong vũ trụ!"),
            DialogueLine(Speaker.BOSS,      "Khà… khà… nhớ soạn sẵn bản nhạc tang—các ngươi sẽ cần sớm thôi!")
        ))
    ),

    3 to LevelScript(
        // Rescued: THIEN_AN — NAME FIXED TO "Thiên An"
        opening = DialogueScene(listOf(
            DialogueLine(Speaker.JACK,      "Thiên An! Dù kết giới photon có bóp méo không gian, anh vẫn bắt được tín hiệu của em!"),
            DialogueLine(Speaker.THIEN_AN,  "Em bình tĩnh đây, Jack. Chỉ cần anh tới, bóng tối cũng phải lùi."),
            DialogueLine(Speaker.BOSS,      "Lời hứa mỏng manh! Hố đen của ta nuốt sạch mọi ánh nhìn si tình!")
        )),
        ending = DialogueScene(listOf(
            DialogueLine(Speaker.JACK,      "Thiên An đã thoát rồi—nắm tay anh, ta bẻ gãy xiềng xích của đêm đen."),
            DialogueLine(Speaker.THIEN_AN,  "Ánh sáng nơi anh đủ ấm để gom cả bầu trời về một phía."),
            DialogueLine(Speaker.BOSS,      "Hừ! Tình yêu à… ta sẽ cho nó bão từ để tắt lịm!")
        ))
    ),

    4 to LevelScript(
        // Rescued: FIREFLIES
        opening = DialogueScene(listOf(
            DialogueLine(Speaker.JACK,      "Đom Đóm, tụ lại! Ánh sáng nhỏ của các em dẫn đường cho cả hạm đội!"),
            DialogueLine(Speaker.FIREFLIES, "Chúng em chớp cánh theo nhịp anh! Tối mấy cũng cứ bừng lên!"),
            DialogueLine(Speaker.BOSS,      "Lấp lánh vô nghĩa! Ta phủ bóng đêm lên từng đốm sáng bé nhỏ!")
        )),
        ending = DialogueScene(listOf(
            DialogueLine(Speaker.JACK,      "Tự do rồi, Đom Đóm! Hãy bay và thắp sao vào từng vệt trời."),
            DialogueLine(Speaker.FIREFLIES, "Có anh, chúng em không lạc nữa! Chớp—chớp—thành dải ngân hà!"),
            DialogueLine(Speaker.BOSS,      "Được lắm… lần sau ta mang theo nhật thực vĩnh cửu!")
        ))
    )
)

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
            
            // Giải phóng avatar bitmaps
            if (::avatarMap.isInitialized) {
                avatarMap.values.forEach { bitmap ->
                    if (!bitmap.isRecycled) {
                        bitmap.recycle()
                    }
                }
            }
            
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

            // Load avatar bitmaps with error handling
            try {
                avatarMap = mapOf(
                    Speaker.JACK to BitmapFactory.decodeResource(resources, R.drawable.avatar_jack),
                    Speaker.BOSS to BitmapFactory.decodeResource(resources, R.drawable.avatar_boss),
                    Speaker.SOL to BitmapFactory.decodeResource(resources, R.drawable.avatar_sol),
                    Speaker.KICM to BitmapFactory.decodeResource(resources, R.drawable.avatar_kicm),
                    Speaker.THIEN_AN to BitmapFactory.decodeResource(resources, R.drawable.avatar_thien_an),
                    Speaker.FIREFLIES to BitmapFactory.decodeResource(resources, R.drawable.avatar_fireflies)
                )
                android.util.Log.d("GameView", "Avatar bitmaps loaded successfully")
            } catch (e: Exception) {
                android.util.Log.e("GameView", "Error loading avatar bitmaps: ${e.message}")
                // Create empty avatarMap as fallback
                avatarMap = emptyMap()
            }

            // Initialize dialogue preloader
            try {
                dialoguePreloader = DialoguePreloader(context)
                android.util.Log.d("GameView", "DialoguePreloader initialized successfully")
            } catch (e: Exception) {
                android.util.Log.e("GameView", "Error initializing DialoguePreloader: ${e.message}")
                e.printStackTrace()
                endGameAndReturnToMenu()
                return
            }
            
            // Initialize story managers - DialogManager MUST be initialized first
            try {
                dialogManager = DialogManager(width, height, avatarMap)
                android.util.Log.d("GameView", "DialogManager initialized successfully")
            } catch (e: Exception) {
                android.util.Log.e("GameView", "Error initializing DialogManager: ${e.message}")
                e.printStackTrace()
                endGameAndReturnToMenu()
                return
            }
            
            introManager = IntroManager(width, height) {
                // Callback khi intro hoàn thành - with error handling
                try {
                    android.util.Log.d("GameView", "Intro completed, starting level 1")
                    isFirstGameStart = false // Đánh dấu intro đã xem
                    // shouldShowStoryContent vẫn là true để hiện dialogue
                    phase = Phase.PLAYING
                    startLevel(1)
                } catch (e: Exception) {
                    android.util.Log.e("GameView", "Error in intro callback: ${e.message}")
                    e.printStackTrace()
                    endGameAndReturnToMenu()
                }
            }

            // Set phase to INTRO chỉ khi lần đầu start game
            if (isFirstGameStart) {
                phase = Phase.INTRO
                android.util.Log.d("GameView", "First game start - showing intro")
            } else {
                phase = Phase.PLAYING
                android.util.Log.d("GameView", "Not first start - skipping intro")
                // Start level 1 immediately when not first time
                startLevel(1)
            }


            // Load offline profile
            val prefs = context.getSharedPreferences("game", Context.MODE_PRIVATE)
            localCoin = prefs.getLong("coins", 0L).toInt()
            coinBeforePlay = localCoin
            thread.running = true
            thread.start()
        }

        fun startLevel(newLevel: Int) {
            android.util.Log.d("GameView", "startLevel($newLevel) called")
            
            // RACE CONDITION FIX: Synchronize level creation to prevent currentLevel access before ready
            synchronized(this) {
                level = newLevel

                // 1. Giải phóng nhạc nền cũ nếu có (tránh trùng tiếng, tránh leak RAM)
                mediaPlayer?.release()
                mediaPlayer = null
                android.util.Log.d("GameView", "Old media player released")

                // 3. Khởi tạo màn chơi theo level như cũ - with error handling
                try {
                    android.util.Log.d("GameView", "Creating Level $level...")
                    currentLevel = when (level) {
                    1 -> {
                        android.util.Log.d("GameView", "Creating Level1 instance...")
                        Level1(context, player, bulletBitmap, itemBitmaps, coinBitmap, backgroundId)
                    }
                    2 -> {
                        android.util.Log.d("GameView", "Creating Level2 instance...")
                        Level2(context, player, bulletBitmap, itemBitmaps, coinBitmap, backgroundId)
                    }
                    3 -> {
                        android.util.Log.d("GameView", "Creating Level3 instance...")
                        Level3(context, player, bulletBitmap, itemBitmaps, coinBitmap, backgroundId)
                    }
                    4 -> {
                        android.util.Log.d("GameView", "Creating Level4 instance...")
                        Level4(context, player, bulletBitmap, itemBitmaps, coinBitmap, backgroundId)
                    }
                    else -> {
                        android.util.Log.d("GameView", "Creating default Level1 instance...")
                        Level1(context, player, bulletBitmap, itemBitmaps, coinBitmap, backgroundId)
                    }
                }
                android.util.Log.d("GameView", "Level $level created, setting screen size...")
                currentLevel.setScreenSize(width, height)
                android.util.Log.d("GameView", "Level $level initialized successfully")
            } catch (e: Exception) {
                android.util.Log.e("GameView", "Error initializing level $level: ${e.message}")
                e.printStackTrace()
                // Fallback to returning to menu
                endGameAndReturnToMenu()
                return
            }

            // Set boss defeat callback để kích hoạt rescue cutscene
            setBossDefeatedCallback()
// Đặt lại vị trí player
            player.x = width / 2 - playerBitmap.width / 2
            player.y = height - playerBitmap.height - 30
            targetX = player.x.toFloat()
            targetY = player.y.toFloat()
            isMoving = false
            // 4. Xử lý cộng xu khi nhặt coin
            currentLevel.onCoinCollected = { amount: Int ->
                localCoin += amount
                val prefs = context.getSharedPreferences("game", Context.MODE_PRIVATE)
                prefs.edit().putLong("coins", localCoin.toLong()).apply()
            }

            // 5. Reset các biến hỗ trợ level
            currentLevel.reset()
            bullets.clear()
            gunMode = GunMode.NORMAL
            gunModeTimer = 0
            autoShootCounter = 0
            isLevelChanging = true
            levelChangeCounter = 0

            // Preload and start opening dialogue when story content is enabled
            if (shouldShowStoryContent) {
                try {
                    STORY[level]?.opening?.let { openingScene ->
                        // Preload assets off-main-thread
                        GlobalScope.launch(Dispatchers.IO) {
                            try {
                                android.util.Log.d("GameView", "Preloading dialogue assets for level $level...")
                                val resources = dialoguePreloader.preload(openingScene, avatarMap)
                                
                                // Switch back to main thread to start dialogue
                                GlobalScope.launch(Dispatchers.Main) {
                                    try {
                                        dialogManager.startScene(openingScene, resources)
                                        android.util.Log.d("GameView", "Opening dialogue started with preloaded resources")
                                    } catch (e: Exception) {
                                        android.util.Log.e("GameView", "Error starting preloaded dialogue: ${e.message}")
                                        // Fallback to old method
                                        dialogManager.queueScene(openingScene)
                                        dialogManager.startIfAny()
                                    }
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("GameView", "Error preloading dialogue assets: ${e.message}")
                                // Fallback to old method
                                GlobalScope.launch(Dispatchers.Main) {
                                    dialogManager.queueScene(openingScene)
                                    dialogManager.startIfAny()
                                }
                            }
                        }
                    }
                    android.util.Log.d("GameView", "Opening dialogue preload initiated (story content enabled)")
                } catch (e: Exception) {
                    // Fallback - nếu dialogue fail, chỉ log và continue game
                    android.util.Log.e("GameView", "Error starting dialogue: ${e.message}")
                }
            } else {
                android.util.Log.d("GameView", "Skipping opening dialogue (story content disabled)")
            }

            // MEMORY LEAK FIX: Cleanup previous level resources
            cleanupPreviousLevel()

            // Setup music LAST - if we reach here, everything else worked
            try {
                if (level in 1..levelMusicResIds.size) {
                    mediaPlayer = MediaPlayer.create(context, levelMusicResIds[level - 1])
                    mediaPlayer?.isLooping = true
                    if (isMusicOn) mediaPlayer?.start()
                    android.util.Log.d("GameView", "Level $level music started successfully")
                }
            } catch (e: Exception) {
                android.util.Log.e("GameView", "Error starting music: ${e.message}")
            }

            android.util.Log.d("GameView", "startLevel($newLevel) completed successfully")
            } // End synchronized block
        }

        private fun setBossDefeatedCallback() {
            // Set boss defeat callback - sử dụng BaseLevel property trực tiếp với error handling
            try {
                currentLevel.onBossDefeated = {
                    try {
                        android.util.Log.d("GameView", "Boss defeated callback triggered! Level: $level, shouldShowStoryContent: $shouldShowStoryContent")
                        if (shouldShowStoryContent) {
                            // Show rescue animation + ending dialogue for story playthrough
                            android.util.Log.d("GameView", "Starting rescue animation...")
                            phase = Phase.RESCUE
                            val speaker = RESCUED_BY_LEVEL[level] ?: Speaker.SOL
                            val startX = width / 2f
                            val startY = height * 0.35f
                            android.util.Log.d("GameView", "Getting avatar dock center...")
                            val dock = dialogManager.avatarDockCenter()
                            val avatarBitmap = avatarMap[speaker]
                            if (avatarBitmap != null) {
                                android.util.Log.d("GameView", "Creating RescueAvatar for speaker: $speaker, from ($startX,$startY) to (${dock.x},${dock.y})")
                                rescueAvatar = RescueAvatar(
                                    avatarBitmap, 
                                    startX, startY, 
                                    dock.x, dock.y, 
                                    durationFrames = 60
                                )
                                rescueTimeout = 0 // Reset timeout
                            } else {
                                android.util.Log.e("GameView", "Avatar bitmap is null for speaker: $speaker")
                                // Fallback - skip rescue, go to dialogue directly
                                phase = Phase.DIALOG
                                STORY[level]?.ending?.let { endingScene ->
                                    dialogManager.queueScene(endingScene)
                                    dialogManager.startIfAny()
                                }
                            }
                            android.util.Log.d("GameView", "RescueAvatar created successfully")
                        } else {
                            // Skip rescue animation and dialogue for subsequent playthroughs
                            android.util.Log.d("GameView", "Skipping story content, moving to next level")
                            if (level < maxLevel) {
                                phase = Phase.PLAYING
                                startLevel(level + 1)
                            } else {
                                endGameAndReturnToMenu()
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("GameView", "Error in boss defeat callback: ${e.message}")
                        // Fallback - go to next level or end game
                        if (level < maxLevel) {
                            phase = Phase.PLAYING
                            startLevel(level + 1)
                        } else {
                            endGameAndReturnToMenu()
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("GameView", "Error setting boss defeat callback: ${e.message}")
            }
        }


        fun endGameAndReturnToMenu() {
            val prefs = context.getSharedPreferences("game", Context.MODE_PRIVATE)
            prefs.edit().putLong("coins", localCoin.toLong()).apply()
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

            // Process input queue - NON-BLOCKING
            processInputQueue()

            // Emergency break để tránh ANR
            updateCallCount++
            if (updateCallCount % 1000 == 0) {
                android.util.Log.w("GameView", "Update called $updateCallCount times, phase: $phase")
            }
            
            // Emergency reset nếu quá nhiều calls trong thời gian ngắn
            if (updateCallCount > 10000) {
                android.util.Log.e("GameView", "Emergency: Too many update calls! Resetting to PLAYING phase")
                phase = Phase.PLAYING
                rescueAvatar = null
                updateCallCount = 0
            }

            // Handle story phases
            if (phase == Phase.INTRO) {
                introManager.update()
                return
            }

            // Early exit if currentLevel not initialized yet (during level transition or intro)
            // Use synchronized to match startLevel synchronization
            if (phase != Phase.INTRO) {
                synchronized(this) {
                    if (!::currentLevel.isInitialized) {
                        android.util.Log.w("GameView", "update() called but currentLevel not initialized yet, phase: $phase")
                        return
                    }
                }
            }

            if (phase == Phase.RESCUE) {
                if (rescueAvatar == null) {
                    android.util.Log.e("GameView", "In RESCUE phase but rescueAvatar is null! Switching to DIALOG")
                    phase = Phase.DIALOG
                    return
                }
                // Only log every 60 frames to avoid spam
                if (rescueTimeout % 60 == 0) {
                    android.util.Log.d("GameView", "In RESCUE phase, updating rescue avatar... (timeout: $rescueTimeout)")
                }
                rescueAvatar?.update()
                rescueTimeout++
                
                // Timeout after 2 seconds (120 frames at 60fps) - reduced for safety
                if (rescueTimeout > 120) {
                    android.util.Log.w("GameView", "Rescue animation timeout! Force completing...")
                    rescueAvatar = null
                    rescueTimeout = 0
                    if (shouldShowStoryContent) {
                        phase = Phase.DIALOG
                        STORY[level]?.ending?.let { endingScene ->
                            dialogManager.queueScene(endingScene)
                            dialogManager.startIfAny()
                        }
                    } else {
                        // Skip to next level
                        if (level < maxLevel) {
                            phase = Phase.PLAYING
                            startLevel(level + 1)
                        } else {
                            endGameAndReturnToMenu()
                        }
                    }
                    return
                }
                
                if (rescueAvatar?.arrived == true) {
                        android.util.Log.d("GameView", "Rescue avatar arrived! shouldShowStoryContent: $shouldShowStoryContent")
                        // Rescue animation hoàn thành, chuyển sang ending dialogue (khi story content enabled)
                        if (shouldShowStoryContent) {
                            android.util.Log.d("GameView", "Starting ending dialogue...")
                            STORY[level]?.ending?.let { endingScene ->
                                dialogManager.queueScene(endingScene)
                                dialogManager.startIfAny()
                            }
                            rescueAvatar = null
                            phase = Phase.DIALOG
                            android.util.Log.d("GameView", "Switched to DIALOG phase")
                        } else {
                            // Skip ending dialogue, go directly to next level
                            android.util.Log.d("GameView", "Skipping ending dialogue...")
                            rescueAvatar = null
                            if (level < maxLevel) {
                                phase = Phase.PLAYING
                                startLevel(level + 1)
                            } else {
                                endGameAndReturnToMenu()
                            }
                        }
                }
                return
            }

            if (dialogManager.isActiveOrHasQueue()) {
                dialogManager.update()
                // Set phase to DIALOG when dialogue is active (for touch handling)
                if (phase == Phase.PLAYING) {
                    android.util.Log.d("GameView", "Switching to DIALOG phase for opening scene")
                    phase = Phase.DIALOG
                }
                return // Pause gameplay while dialogues are showing
            }
            
            // Additional safety check - if we're not in PLAYING phase but should be, and level is not ready
            if (phase == Phase.PLAYING && !::currentLevel.isInitialized) {
                android.util.Log.w("GameView", "In PLAYING phase but currentLevel not ready - waiting...")
                return
            }

            // delay chuyênr level
            if (isLevelChanging) {
                levelChangeCounter++
                if (levelChangeCounter >= levelChangeDuration) {
                    isLevelChanging = false
                }
                return
            }

            // nếu hết mạng thì mở game over - check if currentLevel exists first
            if (::currentLevel.isInitialized && currentLevel.getLives() <= 0) {
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
            // nếu end màn thì mở màn mới hoặc end game - check if currentLevel exists first
            if (::currentLevel.isInitialized && currentLevel.isCompleted()) {
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
                        bullets.add(Bullet(center, bulletY, bulletBitmap, 30, 2000, 90.0))
                        if (isSoundOn) soundPool.play(gunshotSoundId, 1f, 1f, 1, 0, 1f)
                    }

                    GunMode.TRIPLE_PARALLEL -> {
                        bullets.add(Bullet(center, bulletY, bulletBitmap, 30, 2000, 90.0))
                        bullets.add(Bullet(center - offset, bulletY, bulletBitmap, 30, 2, 90.0))
                        bullets.add(Bullet(center + offset, bulletY, bulletBitmap, 30, 2, 90.0))

                        if (isSoundOn) soundPool.play(gunshotSoundId, 1f, 1f, 1, 0, 1f)
                    }

                    GunMode.TRIPLE_SPREAD -> {
                        bullets.add(Bullet(center, bulletY, bulletBitmap, 30, 2000, 90.0))
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

            // update level - check if currentLevel exists first
            if (::currentLevel.isInitialized) {
                currentLevel.update(bullets)

                currentLevel.pickedGunMode?.let {
                    gunMode = it
                    gunModeTimer = 0
                    currentLevel.pickedGunMode = null
                }
            }
            // item đổi súng
            if (gunMode != GunMode.NORMAL) {
                gunModeTimer++
                if (gunModeTimer > gunModeDuration) gunMode = GunMode.NORMAL
            }
            missile?.update()
            // Khi tên lửa vừa bắt đầu nổ, xóa sạch quái và trừ máu boss cho mọi level
            if (missile != null && missile!!.isExploding && missile!!.explosionFrame == 1 && ::currentLevel.isInitialized) {
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
                if (::currentLevel.isInitialized) {
                    currentLevel.manaCount = 0
                }
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
            
            // Vẽ gameplay chỉ khi không phải INTRO phase và currentLevel đã được khởi tạo
            if (phase != Phase.INTRO && ::currentLevel.isInitialized) {
                currentLevel.draw(canvas, bullets)
            }

            val paint = uiPaint
            
            // Chỉ vẽ UI gameplay khi không phải INTRO phase
            if (phase != Phase.INTRO && ::currentLevel.isInitialized) {
                val currentMana = currentLevel.manaCount
                val manaNeeded = currentLevel.manaNeededForMissile
                
                // MEMORY LEAK FIX: Cache life icon bitmap instead of creating new one every frame
                if (lifeIconBitmap == null) {
                    lifeIconBitmap = Bitmap.createScaledBitmap(playerBitmap, 60, 60, true)
                }
                
                for (i in 0 until currentLevel.getLives()) {
                    canvas.drawBitmap(lifeIconBitmap!!, 40f + i * 70, 80f, paint)
                }
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

            // Vẽ nút tên lửa/missile (chỉ khi không phải INTRO)
            if (phase != Phase.INTRO && ::currentLevel.isInitialized) {
                val currentMana = currentLevel.manaCount
                val manaNeeded = currentLevel.manaNeededForMissile
                
                val btnX = width - missileButtonBitmapScaled.width - 40
                val btnY = height - missileButtonBitmapScaled.height - 40
                canvas.drawBitmap(missileButtonBitmapScaled, btnX.toFloat(), btnY.toFloat(), null)

                // Thông số vòng tròn mana cho tên lửa
                val cx = btnX + missileButtonBitmapScaled.width / 2f
                val cy = btnY + missileButtonBitmapScaled.height / 2f
                val radius = missileButtonBitmapScaled.width / 2f + 16f
                val manaStrokeWidth = 14f
                val sweep = 360f / manaNeeded

                // MEMORY LEAK FIX: Use cached paint objects with dynamic stroke width
                manaFilledPaint.strokeWidth = manaStrokeWidth
                manaEmptyPaint.strokeWidth = manaStrokeWidth
                val paintFilled = manaFilledPaint
                val paintEmpty = manaEmptyPaint

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
            }

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

            // Vẽ tên lửa nếu có (chỉ khi không phải INTRO)
            if (phase != Phase.INTRO) {
                missile?.draw(canvas, missileExplosionBitmap)
            }

            // Story elements rendering
            if (phase == Phase.INTRO) {
                introManager.draw(canvas)
            } else if (phase == Phase.RESCUE) {
                rescueAvatar?.draw(canvas)
            } else if (phase == Phase.DIALOG) {
                dialogManager.draw(canvas)
            }

            paint.textAlign = Paint.Align.LEFT

            // Vẽ hiệu ứng chuyển màn, game over, menu game over
            if (isLevelChanging) {
                paint.textSize = 80f
                paint.color = Color.YELLOW
                paint.textAlign = Paint.Align.CENTER
                val msg = when {
                    ::currentLevel.isInitialized && currentLevel.getLives() <= 0 -> "GAME OVER"
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

        /**
         * Process input queue on GameThread - NON-BLOCKING
         */
        private fun processInputQueue() {
            while (!inputQueue.isEmpty()) {
                val event = inputQueue.poll() ?: break
                
                // Record input processing start
                performanceMonitor.recordInputProcessed()
                
                when (event) {
                    is InputEvent.NextDialogue -> {
                        if (dialogManager.isActiveOrHasQueue()) {
                            dialogManager.handleInput(event)
                        }
                    }
                    is InputEvent.Touch -> {
                        if (dialogManager.isActiveOrHasQueue()) {
                            dialogManager.handleInput(event)
                        }
                    }
                    is InputEvent.LongPress -> {
                        if (dialogManager.isActiveOrHasQueue()) {
                            dialogManager.handleInput(event)
                        }
                    }
                }
            }
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            // Story input handling priority
            if (phase == Phase.INTRO) {
                return introManager.onTouch(event)
            }

            if (phase == Phase.DIALOG && dialogManager.isActiveOrHasQueue()) {
                android.util.Log.d("GameView", "Enqueueing touch event for DialogManager in DIALOG phase")
                // NON-BLOCKING: Enqueue touch event instead of processing directly
                performanceMonitor.recordInput()
                inputQueue.enqueue(InputEvent.Touch(event.x, event.y, event.action, event.eventTime))
                return true
            }

            if (phase == Phase.RESCUE) {
                // Consume input during rescue cutscene
                return true
            }

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
            if (missileBtnRect != null && event.action == MotionEvent.ACTION_DOWN && ::currentLevel.isInitialized) {
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
                        phase = Phase.PLAYING // Reset phase
                        rescueAvatar = null // Clear rescue avatar
                        shouldShowStoryContent = false // Không hiện story content khi retry

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
                var frameCount = 0
                while (running) {
                    try {
                        val canvas: Canvas? = surfaceHolder.lockCanvas()
                        if (canvas != null) {
                            synchronized(surfaceHolder) {
                                gameView.update()
                                gameView.draw(canvas)
                            }
                            surfaceHolder.unlockCanvasAndPost(canvas)
                        }
                        
                        // Record frame timing for performance monitoring
                        performanceMonitor.recordFrame()
                        
                        frameCount++
                        // Log performance every 5 seconds
                        if (frameCount % 300 == 0) {
                            val fps = performanceMonitor.getCurrentFPS()
                            val latency = performanceMonitor.getAverageInputLatency()
                            val isAcceptable = performanceMonitor.isPerformanceAcceptable()
                            android.util.Log.d("GameThread", "Frame $frameCount, phase: ${gameView.phase}, FPS: $fps, Latency: ${latency}ms, Acceptable: $isAcceptable")
                        }
                        
                        sleep(16)
                    } catch (e: Exception) {
                        android.util.Log.e("GameThread", "Error in game loop: ${e.message}")
                        e.printStackTrace()
                        // Emergency sleep to prevent tight loop
                        sleep(100)
                    }
                }
            }
        }
    }