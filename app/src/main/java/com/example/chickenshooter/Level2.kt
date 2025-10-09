package com.example.chickenshooter.levels

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import com.example.chickenshooter.*
import com.example.chickenshooter.utils.CollisionUtils
import android.graphics.Paint
import com.example.chickenshooter.R
import com.example.chickenshooter.utils.SpriteUtils
import kotlin.random.Random
import android.graphics.Color
class Level2(
    context: Context,
    player: Player,
    bulletBitmap: Bitmap,
    itemBitmaps: List<Bitmap>,
    coinBmp: Bitmap,
    private val backgroundId: Int
) : BaseLevel(context, player, bulletBitmap, itemBitmaps, coinBmp) {

    private val background = BitmapFactory.decodeResource(context.resources, backgroundId)
    private val chickenBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.chicken2)
    private val eggBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.egg)
    private val shieldBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.shield_item)
    private val healthItemBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.blood)

    private val scaledShieldBitmap = Bitmap.createScaledBitmap(
        shieldBitmap,
        (shieldBitmap.width * 0.07).toInt(),
        (shieldBitmap.height * 0.07).toInt(),
        true
    )
    private val scaledHealthItemBitmap = Bitmap.createScaledBitmap(
        healthItemBitmap,
        (healthItemBitmap.width * 0.07).toInt(),
        (healthItemBitmap.height * 0.07).toInt(),
        true
    )
    
    init {
        // Initialize BaseLevel systems
        setShieldBitmap(scaledShieldBitmap)
        setHealthItemBitmap(scaledHealthItemBitmap)
        
        // Set up callbacks
        onHealthCollected = { lives++ }
        onEggHit = { 
            lives--
            player.hit(playerExplosionFrames)
        }
    }

    private val playerExplosionFrames = SpriteUtils.splitSpriteSheet(
        context,
        R.drawable.explosion_animation_v1,
        rows = 8,
        cols = 8
    )

    private val scaledChickenBitmap = Bitmap.createScaledBitmap(
        chickenBitmap,
        chickenBitmap.width / 13,
        chickenBitmap.height / 13,
        true
    )
    private val manaBitmap = Bitmap.createScaledBitmap(
        BitmapFactory.decodeResource(context.resources, R.drawable.mana),
        scaledChickenBitmap.width,
        scaledChickenBitmap.height,
        true
    )
    override val coinBitmap: Bitmap = Bitmap.createScaledBitmap(
        coinBmp,
        scaledChickenBitmap.width * 4 / 5,
        scaledChickenBitmap.height * 4 / 5,
        true
    )
    private val bossBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.boss_chicken2)
    private val bossScaledBitmap = Bitmap.createScaledBitmap(
        bossBitmap,
        bossBitmap.width * 3 / 20,
        bossBitmap.height * 3 / 20,
        true
    )
    private val scaledEggBitmap = Bitmap.createScaledBitmap(
        eggBitmap,
        coinBitmap.width + 5,
        coinBitmap.height + 5,
        true
    )

    private val allProjectiles = mutableListOf<ChickenProjectile>()
    internal val chickens = mutableListOf<Chicken>()
    var boss: BossChicken? = null

    // --- WAVE / BOSS STATE ---
    private var isBossSpawned = false
    private var currentWave = 0
    private val maxWaves = 5 // Cố định 5 wave
    private var allWavesCompleted = false // Đảm bảo tất cả 5 wave đã hoàn thành

    private val wavePatterns = listOf(MoveType.BOUNCE, MoveType.SINE, MoveType.ZIGZAG, MoveType.V, MoveType.SPIRAL)
    var waveTimer = 0

    // (2) cap số gà đang hoạt động
    private val maxActiveChickens = 16

    // --- DIFFICULTY / PLAYER STATE ---
    private val baseChickenSpeed = 5f
    private val baseChickenHp = 3
    private var lives = 3
    private var isLevelFinished = false

    var enemiesKilled = 0
    var waveEnemiesKilled = 0

    override var pickedGunMode: GunMode? = null

    override fun update(bullets: MutableList<Bullet>) {
        if (isLevelFinished) return

        scrollBackground?.update()
        
        // Flag để tránh trừ nhiều mạng cùng lúc
        var playerHitThisFrame = false

        // === Điều khiển spawn wave ===
        if (!isBossSpawned && currentWave < maxWaves) {
            // Spawn wave hiện tại nếu chưa spawn
            if (waveTimer == 0) {
                startWave(currentWave)
                waveTimer = 1
            }
            
            // Tăng wave timer mỗi frame
            waveTimer++
            
            // Kiểm tra điều kiện hoàn thành wave
            val requiredKillsForWave = getRequiredKillsForWave(currentWave)
            if (waveEnemiesKilled >= requiredKillsForWave && chickens.isEmpty()) {
                // Wave hoàn thành, chuyển ngay sang wave tiếp theo
                currentWave++
                waveTimer = 0
                waveEnemiesKilled = 0
                
                // Kiểm tra xem đã hoàn thành tất cả 5 wave chưa
                if (currentWave >= maxWaves) {
                    allWavesCompleted = true
                }
            }
            
            // FALLBACK: Nếu wave bị stuck quá lâu (không có quái và không spawn), force spawn wave tiếp theo
            if (chickens.isEmpty() && waveTimer > 300) { // 5 giây timeout
                android.util.Log.w("Level2", "Wave stuck detected, forcing next wave. Current wave: $currentWave, Timer: $waveTimer")
                currentWave++
                waveTimer = 0
                waveEnemiesKilled = 0
                
                if (currentWave >= maxWaves) {
                    allWavesCompleted = true
                }
            }
        } else if (allWavesCompleted && !isBossSpawned) {
            // Đã hoàn thành tất cả 5 wave, kiểm tra tất cả quái đã bị tiêu diệt hết chưa
            if (chickens.isEmpty() && boss == null) {
                spawnBoss()
            }
        }

        // === Update chickens + gom projectiles ===
        val playerCenterX = player.x + player.getRect().width() / 2f
        val playerCenterY = player.y + player.getRect().height() / 2f
        for (chicken in chickens) {
            chicken.update(playerCenterX, playerCenterY)
            if (chicken.projectiles.isNotEmpty()) {
                allProjectiles.addAll(chicken.projectiles)
                chicken.projectiles.clear()
            }
        }

        // === ChickenProjectile vs Player ===
        val hitProjectiles = allProjectiles.filter {
            CollisionUtils.isColliding(it.getRect(), player.getRect())
        }
        if (hitProjectiles.isNotEmpty() && !playerHitThisFrame) {
            if (!player.isInvulnerable()) {
                player.hit(playerExplosionFrames)
                lives--
                playerHitThisFrame = true
            }
        }
        allProjectiles.removeAll(hitProjectiles)

        // Update & cắt projectile out-of-screen
        val screenW = context.resources.displayMetrics.widthPixels
        val screenH = context.resources.displayMetrics.heightPixels
        val projIt = allProjectiles.iterator()
        while (projIt.hasNext()) {
            val p = projIt.next()
            p.update()
            if (p.y > screenH) projIt.remove()
        }

        // ... (tất cả logic xử lý items, bullets, collision, boss, ... giữ nguyên như trước) ...
        // --- Dưới đây chỉ là các phần còn lại giữ nguyên, đã rút gọn cho phần spawn gà theo đợt là chính ---

        // Bullet - Chicken collision
        handleBulletChickenCollision(chickens, bullets) { chicken ->
            enemiesKilled++
            waveEnemiesKilled++
            if ((0..99).random() < 5) spawnShield(chicken.x.toInt(), chicken.y.toInt())
            if ((0..99).random() < 5) spawnHealthItem(chicken.x.toInt(), chicken.y.toInt())
            if ((0..99).random() < 4) {
                val itemType = ItemType.values()[(0..2).random()]
                spawnGunItem(chicken.x.toInt(), chicken.y.toInt(), itemType)
            }
            if (Math.random() < 0.075) spawnMana(chicken.x.toInt(), chicken.y.toInt(), manaBitmap, 8)
            spawnCoin(chicken.x.toInt(), chicken.y.toInt(), chicken.bitmap.width, chicken.bitmap.height)
        }

        // Player - Chicken collision
        handlePlayerChickenCollision(chickens, 
            onPlayerHit = { 
                if (!playerHitThisFrame && !player.isInvulnerable()) {
                    lives--
                    player.hit(playerExplosionFrames)
                    playerHitThisFrame = true
                }
            },
            onEnemyKilled = { enemiesKilled++ }
        )

        // Update BaseLevel systems
        updateGunItems()

        // Update coins, mana, shields, health items
        updateCoins()
        updateMana()
        updateShields()
        updateHealthItems()

        // Boss logic
        boss?.let { b ->
            b.update(System.currentTimeMillis(), eggs)
        }
        handleBossCollision(boss, bullets,
            onPlayerHit = { 
                if (!playerHitThisFrame && !player.isInvulnerable()) {
                    lives--
                    player.hit(playerExplosionFrames)
                    playerHitThisFrame = true
                }
            },
            onBossDefeated = {
                isLevelFinished = true
                try {
                    onBossDefeated?.invoke()
                } catch (_: Exception) {}
            }
        )

        updateEggs()

        if (lives <= 0) isLevelFinished = true

        player.update()
    }

    // === WAVE MANAGEMENT FUNCTIONS ===
    private fun getRequiredKillsForWave(wave: Int): Int {
        return when (wave) {
            0 -> 6  // Wave 1: 6 gà (tăng x2 từ 3)
            1 -> 10 // Wave 2: 10 gà (tăng x2 từ 5)
            2 -> 14 // Wave 3: 14 gà (tăng x2 từ 7)
            3 -> 18 // Wave 4: 18 gà (tăng x2 từ 9)
            4 -> 24 // Wave 5: 24 gà (tăng x2 từ 12)
            else -> 24
        }
    }
    
    private fun startWave(wave: Int) {
        val moveType = wavePatterns[wave % wavePatterns.size]
        spawnWave(moveType, wave)
    }
    

    private fun spawnWave(moveType: MoveType, wave: Int) {
        // Progressive difficulty scaling theo wave (0-4)
        val numChickens = getRequiredKillsForWave(wave) // Số gà = số kill cần thiết
        val speedScale = baseChickenSpeed + (wave * 0.8f) // Tăng 0.8f mỗi wave
        val hpScale = baseChickenHp + (wave / 2) // Tăng HP mỗi 2 wave

        val screenW = context.resources.displayMetrics.widthPixels
        val screenH = context.resources.displayMetrics.heightPixels
        val availableWidth = screenW - scaledChickenBitmap.width
        val spacing = if (numChickens > 1) availableWidth.toFloat() / (numChickens - 1) else 0f

        val formationType = wave % 4
        when (formationType) {
            0 -> { // HÀNG NGANG ĐỀU
                for (i in 0 until numChickens) {
                    val x = i * spacing
                    chickens.add(
                        Chicken(
                            x = x,
                            y = 0f,
                            bitmap = scaledChickenBitmap,
                            speed = speedScale,
                            moveType = moveType,
                            hp = hpScale,
                            screenWidth = screenW,
                            screenHeight = screenH
                        )
                    )
                }
            }
            1 -> { // HÌNH CHỮ V NHỌN
                val mid = (numChickens - 1) / 2f
                val vHeight = 200f + wave * 6f // chữ V sâu dần
                for (i in 0 until numChickens) {
                    val x = i * spacing
                    val dx = kotlin.math.abs(i - mid)
                    val y = dx / kotlin.math.max(1f, mid) * vHeight
                    chickens.add(
                        Chicken(
                            x = x,
                            y = y,
                            bitmap = scaledChickenBitmap,
                            speed = speedScale,
                            moveType = moveType,
                            hp = hpScale,
                            screenWidth = screenW,
                            screenHeight = screenH
                        )
                    )
                }
            }
            2 -> { // ZIGZAG
                val offset = 50f + wave * 3f
                for (i in 0 until numChickens) {
                    val x = i * spacing
                    val y = if (i % 2 == 0) 0f else offset
                    chickens.add(
                        Chicken(
                            x = x,
                            y = y,
                            bitmap = scaledChickenBitmap,
                            speed = speedScale,
                            moveType = moveType,
                            hp = hpScale,
                            screenWidth = screenW,
                            screenHeight = screenH
                        )
                    )
                }
            }
            3 -> { // SIN ĐẦU
                val amplitude = 60f + wave * 2.5f
                val freq = Math.PI / kotlin.math.max(1, numChickens)
                for (i in 0 until numChickens) {
                    val x = i * spacing
                    val y = amplitude * (1 + Math.sin(i * freq)).toFloat()
                    chickens.add(
                        Chicken(
                            x = x,
                            y = y,
                            bitmap = scaledChickenBitmap,
                            speed = speedScale,
                            moveType = moveType,
                            hp = hpScale,
                            screenWidth = screenW,
                            screenHeight = screenH
                        )
                    )
                }
            }
        }
    }

    private fun spawnBoss() {
        val screenW = context.resources.displayMetrics.widthPixels
        val screenH = context.resources.displayMetrics.heightPixels
        boss = BossChicken(
            x = (screenW - bossScaledBitmap.width) / 2,
            y = 50,
            bitmap = bossScaledBitmap,
            hp = 200,  // Boss Level 2 khó hơn Level 1 (200 -> 300)
            vx = 3,
            vy = 2,
            eggBitmap = scaledEggBitmap, // dùng bitmap đã scale
            screenWidth = screenW,
            screenHeight = screenH,
            eggCount = 4 // Level2: 4 tia
        )
        isBossSpawned = true
    }

    override fun draw(canvas: Canvas, bullets: List<Bullet>, backgroundY: Float) {
        scrollBackground?.draw(canvas)
        player.draw(canvas)
        chickens.forEach { it.draw(canvas) }
        bullets.forEach { it.draw(canvas) }
        drawGunItems(canvas)
        drawShields(canvas)
        drawHealthItems(canvas)
        player.draw(canvas)
        drawCoins(canvas)
        drawMana(canvas)
        drawEggs(canvas)
        for (proj in allProjectiles) {
            proj.draw(canvas)
        }
        boss?.draw(canvas)
        boss?.let { b ->
            val barWidth = canvas.width * 2 / 3
            val barHeight = 30
            val left = (canvas.width - barWidth) / 2
            val top = 40
            val hpPercent = b.hp.toFloat() / b.maxHp
            val hpBarCurrentWidth = (barWidth * hpPercent).toInt()
            val paintBg = Paint().apply { color = Color.DKGRAY }
            val paintHp = Paint().apply { color = Color.RED }
            val paintBorder = Paint().apply {
                color = Color.WHITE
                style = Paint.Style.STROKE
                strokeWidth = 4f
            }
            val paintText = Paint().apply {
                color = Color.WHITE
                textSize = 32f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawRect(left.toFloat(), top.toFloat(), (left + barWidth).toFloat(), (top + barHeight).toFloat(), paintBg)
            canvas.drawRect(left.toFloat(), top.toFloat(), (left + hpBarCurrentWidth).toFloat(), (top + barHeight).toFloat(), paintHp)
            canvas.drawRect(left.toFloat(), top.toFloat(), (left + barWidth).toFloat(), (top + barHeight).toFloat(), paintBorder)
            canvas.drawText("Boss HP: ${b.hp}/${b.maxHp}", canvas.width / 2f, (top + barHeight + 32).toFloat(), paintText)
        }
    }

    override fun isCompleted(): Boolean = isLevelFinished
    
    override fun isBossSpawned(): Boolean = isBossSpawned

    override fun reset() {
        super.cleanup() // Clear BaseLevel systems
        chickens.clear()
        boss = null
        isBossSpawned = false
        currentWave = 0
        allWavesCompleted = false
        lives = 3
        isLevelFinished = false
        waveTimer = 0
        enemiesKilled = 0
        waveEnemiesKilled = 0
        pickedGunMode = null
        saveCoinsToSystem()
    }

    override fun canUseMissile(): Boolean = manaCount >= manaNeededForMissile && !isLevelFinished

    override fun consumeManaForMissile() {
        manaCount -= manaNeededForMissile
    }
    override fun getBackground(): Bitmap = background
    override fun getLives(): Int = lives

    override fun cleanup() {
        try {
            super.cleanup()
            chickens.clear()
            boss = null
        } catch (_: Exception) {}
    }
}