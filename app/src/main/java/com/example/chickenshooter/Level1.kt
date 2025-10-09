package com.example.chickenshooter.levels

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color
import com.example.chickenshooter.*
import com.example.chickenshooter.utils.CollisionUtils
import com.example.chickenshooter.R
import com.example.chickenshooter.utils.SpriteUtils
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class Level1(
    context: Context,
    player: Player,
    bulletBitmap: Bitmap,
    itemBitmaps: List<Bitmap>,
    coinBmp: Bitmap,
    private val backgroundId: Int
) : BaseLevel(context, player, bulletBitmap, itemBitmaps, coinBmp) {

    private val background = BitmapFactory.decodeResource(context.resources, backgroundId)
    private val chickenBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.chicken1)
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
    private val bossBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.boss_chicken1)
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

    private val screenW = context.resources.displayMetrics.widthPixels
    private val screenH = context.resources.displayMetrics.heightPixels

    private val allProjectiles = mutableListOf<ChickenProjectile>()
    internal val chickens = mutableListOf<Chicken>()
    var boss: BossChicken? = null

    // --- WAVE / BOSS STATE ---
    private var isBossSpawned = false
    private var bossReady = false // (5) chỉ spawn boss sau khi clear wave

    private val wavePatterns = listOf(MoveType.BOUNCE, MoveType.SINE, MoveType.ZIGZAG)
    private var waveIndex = 0
    private var waveTimer = 0
    private var baseWaveInterval = 120 // ~2s @ 60fps, có thể tinh chỉnh theo waveIndex nếu cần

    // (2) cap số gà đang hoạt động
    private val maxActiveChickens = 16

    // --- DIFFICULTY / PLAYER STATE ---
    private val baseChickenSpeed = 5f
    private val baseChickenHp = 3
    private var lives = 3
    private var isLevelFinished = false

    private var enemiesKilled = 0
    private val requiredKills = 15 // mốc hạ quái để chuẩn bị gặp boss

    override var pickedGunMode: GunMode? = null

    override fun update(bullets: MutableList<Bullet>) {
        if (isLevelFinished) return

        scrollBackground?.update()

        // === Điều khiển spawn wave ===
        if (!isBossSpawned) {
            // (5) Khi đủ kill → bossReady, KHÔNG spawn wave mới nữa
            if (!bossReady && enemiesKilled >= requiredKills) {
                bossReady = true
            }

            // Nếu chưa bossReady, tiếp tục spawn theo timer & theo cap số gà
            if (!bossReady) {
                waveTimer++
                // có thể giảm nhịp dần nhẹ theo waveIndex (optional):
                val dynamicInterval = max(80, baseWaveInterval - (waveIndex * 2)) // sàn 80
                if (waveTimer >= dynamicInterval && chickens.size < maxActiveChickens) {
                    spawnWave(wavePatterns[waveIndex % wavePatterns.size])
                    waveIndex++
                    waveTimer = 0
                }
            } else {
                // bossReady == true → chờ clear hết gà rồi mới spawn boss
                if (chickens.isEmpty() && boss == null) {
                    spawnBoss()
                }
            }
        }

        // === Update chickens + gom projectiles (đã fix double-update) ===
        run {
            val playerCenterX = player.x + player.getRect().width() / 2f
            val playerCenterY = player.y + player.getRect().height() / 2f

            for (chicken in chickens) {
                chicken.update(playerCenterX, playerCenterY)
                if (chicken.projectiles.isNotEmpty()) {
                    allProjectiles.addAll(chicken.projectiles)
                    chicken.projectiles.clear()
                }
            }
        }

        // === ChickenProjectile vs Player ===
        val hitProjectiles = allProjectiles.filter {
            CollisionUtils.isColliding(it.getRect(), player.getRect())
        }
        for (projectile in hitProjectiles) {
            if (!player.hasShield) {
                lives--
                player.hit(playerExplosionFrames)
            }
        }
        allProjectiles.removeAll(hitProjectiles)

        // Update & cắt projectile out-of-screen
        val projIt = allProjectiles.iterator()
        while (projIt.hasNext()) {
            val p = projIt.next()
            p.update()
            if (p.y > screenH) projIt.remove()
        }

        // === Bullet vs Chicken ===
        handleBulletChickenCollision(chickens, bullets) { chicken ->
            enemiesKilled++
            if ((0..99).random() < 3) spawnShield(chicken.x.toInt(), chicken.y.toInt())
            if ((0..99).random() < 3) spawnHealthItem(chicken.x.toInt(), chicken.y.toInt())
            if ((0..99).random() < 3) {
                val itemType = ItemType.values()[(0..2).random()]
                spawnGunItem(chicken.x.toInt(), chicken.y.toInt(), itemType)
            }
            if (Math.random() < 0.03) spawnMana(chicken.x.toInt(), chicken.y.toInt(), manaBitmap, 8)
            spawnCoin(chicken.x.toInt(), chicken.y.toInt(), chicken.bitmap.width, chicken.bitmap.height)
        }

        // === Player vs Chicken ===
        handlePlayerChickenCollision(chickens, 
            onPlayerHit = { 
                lives--
                player.hit(playerExplosionFrames)
            },
            onEnemyKilled = { enemiesKilled++ }
        )

        // === Update BaseLevel systems ===
        updateGunItems()

        // === Update/cleanup đồ rơi & tài nguyên ===
        updateCoins()
        updateMana()
        updateShields()
        updateHealthItems()

        // === Boss logic ===
        boss?.let { b ->
            b.update(System.currentTimeMillis(), eggs)
        }
        handleBossCollision(boss, bullets,
            onPlayerHit = { 
                lives--
                player.hit(playerExplosionFrames)
            },
            onBossDefeated = {
                isLevelFinished = true
                try {
                    onBossDefeated?.invoke()
                } catch (_: Exception) { }
            }
        )

        // === Egg từ boss ===
        updateEggs()

        if (lives <= 0) isLevelFinished = true

        player.update()
    }

    /**
     * Spawn 1 đợt gà, có leo thang độ khó theo waveIndex
     */
    private fun spawnWave(moveType: MoveType) {
        // (3) Scaling số lượng / speed / hp
        val numChickens = min(16, 6 + min(2 + waveIndex, 10)) // 6 → 16
        val speedScale = baseChickenSpeed + (waveIndex * 0.15f) // tăng nhẹ
        val hpScale = baseChickenHp + (waveIndex / 3)           // mỗi 3 wave +1 HP

        val availableWidth = screenW - scaledChickenBitmap.width
        val spacing = if (numChickens > 1) availableWidth.toFloat() / (numChickens - 1) else 0f

        val formationType = waveIndex % 4
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
                val vHeight = 200f + waveIndex * 6f // chữ V sâu dần
                for (i in 0 until numChickens) {
                    val x = i * spacing
                    val dx = abs(i - mid)
                    val y = dx / max(1f, mid) * vHeight
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
                val offset = 50f + waveIndex * 3f
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
                val amplitude = 60f + waveIndex * 2.5f
                val freq = Math.PI / max(1, numChickens)
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
        boss = BossChicken(
            x = (screenW - bossScaledBitmap.width) / 2,
            y = 50,
            bitmap = bossScaledBitmap,
            hp = 200,
            vx = 6,
            vy = 3,
            eggBitmap = scaledEggBitmap, // dùng bitmap đã scale
            screenWidth = screenW,
            screenHeight = screenH
        )
        isBossSpawned = true
    }

    override fun draw(canvas: Canvas, bullets: List<Bullet>, backgroundY: Float) {
        scrollBackground?.draw(canvas)

        // order vẽ: quái -> đạn -> đồ rơi -> khiên/máu -> trứng/đạn gà -> boss -> player -> UI
        chickens.forEach { it.draw(canvas) }
        bullets.forEach { it.draw(canvas) }
        drawGunItems(canvas)
        drawShields(canvas)
        drawHealthItems(canvas)
        drawEggs(canvas)
        for (proj in allProjectiles) {
            proj.draw(canvas)
        }
        boss?.draw(canvas)

        // vẽ player một lần
        player.draw(canvas)

        // UI: coin/mana & boss HP bar
        drawCoins(canvas)
        drawMana(canvas)

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

            canvas.drawRect(
                left.toFloat(), top.toFloat(),
                (left + barWidth).toFloat(), (top + barHeight).toFloat(), paintBg
            )
            canvas.drawRect(
                left.toFloat(), top.toFloat(),
                (left + hpBarCurrentWidth).toFloat(), (top + barHeight).toFloat(), paintHp
            )
            canvas.drawRect(
                left.toFloat(), top.toFloat(),
                (left + barWidth).toFloat(), (top + barHeight).toFloat(), paintBorder
            )
            canvas.drawText(
                "Boss HP: ${b.hp}/${b.maxHp}",
                canvas.width / 2f,
                (top + barHeight + 32).toFloat(),
                paintText
            )
        }
    }

    override fun isCompleted(): Boolean = isLevelFinished
    override fun isBossSpawned(): Boolean = isBossSpawned

    override fun reset() {
        super.cleanup() // Clear BaseLevel systems
        chickens.clear()
        boss = null
        isBossSpawned = false
        bossReady = false
        lives = 3
        isLevelFinished = false
        waveIndex = 0
        waveTimer = 0
        enemiesKilled = 0
        pickedGunMode = null
        saveCoinsToSystem()
    }

    override fun canUseMissile(): Boolean =
        manaCount >= manaNeededForMissile && !isLevelFinished

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
        } catch (_: Exception) { }
    }
}
