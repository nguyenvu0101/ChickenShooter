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

    // --- SPAWN / BOSS STATE ---
    private var isBossSpawned = false
    var spawnTimer = 0
    private val spawnInterval = 120 // 2 giây (60 FPS * 2)
    private val targetEnemies = 66 // Số gà cần tiêu diệt để spawn boss
    private val formationPatterns = listOf(0, 1, 2, 3) // 4 formation patterns

    // (2) cap số gà đang hoạt động
    private val maxActiveChickens = 32

    // --- DIFFICULTY / PLAYER STATE ---
    private val baseChickenSpeed = 5f
    private val baseChickenHp = 3
    private var lives = 3
    private var isLevelFinished = false

    var enemiesKilled = 0

    override var pickedGunMode: GunMode? = null

    override fun update(bullets: MutableList<Bullet>) {
        if (isLevelFinished) return

        scrollBackground?.update()
        
        // Flag để tránh trừ nhiều mạng cùng lúc
        var playerHitThisFrame = false

        // === BUG FIX: Xóa gà khi ra khỏi màn hình (PHẢI ĐẶT TRƯỚC LOGIC SPAWN BOSS) ===
        val screenW = context.resources.displayMetrics.widthPixels
        val screenH = context.resources.displayMetrics.heightPixels
        val chickenIt = chickens.iterator()
        while (chickenIt.hasNext()) {
            val chicken = chickenIt.next()
            // Xóa gà khi bay ra khỏi màn hình (y > screenHeight + 100 để tránh xóa quá sớm)
            if (chicken.y > screenH + 100) {
                chickenIt.remove()
                android.util.Log.d("Level2", "Removed chicken at y=${chicken.y}, screenH=$screenH")
            }
        }

        // === Điều khiển spawn formation ===
        if (!isBossSpawned && enemiesKilled < targetEnemies) {
            // Tăng spawn timer mỗi frame
            spawnTimer++
            
            // Spawn formation mỗi 2 giây
            if (spawnTimer >= spawnInterval) {
                val randomFormation = formationPatterns.random()
                val randomCount = (8..16).random()
                android.util.Log.d("Level2", "Spawning formation: enemiesKilled=$enemiesKilled, targetEnemies=$targetEnemies, count=$randomCount")
                spawnFormation(randomFormation, randomCount)
                spawnTimer = 0
            }
        } else if (enemiesKilled >= targetEnemies && !isBossSpawned) {
            // Đã đủ 66 gà, kiểm tra tất cả quái đã bị tiêu diệt hết chưa
            android.util.Log.d("Level2", "Checking boss spawn: enemiesKilled=$enemiesKilled, targetEnemies=$targetEnemies, chickens.size=${chickens.size}, isBossSpawned=$isBossSpawned")
            if (chickens.isEmpty() && boss == null) {
                android.util.Log.d("Level2", "Spawning boss: enemiesKilled=$enemiesKilled, chickens.size=${chickens.size}")
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
        val projIt = allProjectiles.iterator()
        while (projIt.hasNext()) {
            val p = projIt.next()
            p.update()
            if (p.y > screenH) projIt.remove()
        }

      

        // Bullet - Chicken collision
        handleBulletChickenCollision(chickens, bullets) { chicken ->
            enemiesKilled++
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

    // === FORMATION SPAWN FUNCTIONS ===
    private fun spawnFormation(formationType: Int, numChickens: Int) {
        // Random move type cho mỗi formation
        val moveTypes = listOf(MoveType.BOUNCE, MoveType.SINE, MoveType.ZIGZAG, MoveType.V, MoveType.SPIRAL)
        val moveType = moveTypes.random()
        
        // Progressive difficulty scaling theo số gà đã spawn
        val difficultyScale = (enemiesKilled / 10f).coerceAtMost(5f) // Max 5x difficulty
        val speedScale = baseChickenSpeed + (difficultyScale * 0.5f)
        val hpScale = baseChickenHp + (difficultyScale / 2).toInt()

        val screenW = context.resources.displayMetrics.widthPixels
        val screenH = context.resources.displayMetrics.heightPixels
        val availableWidth = screenW - scaledChickenBitmap.width
        val spacing = if (numChickens > 1) availableWidth.toFloat() / (numChickens - 1) else 0f

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
                val vHeight = 200f + difficultyScale * 10f
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
                val offset = 50f + difficultyScale * 5f
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
                val amplitude = 60f + difficultyScale * 5f
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
            eggCount = 4 
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
        spawnTimer = 0
        lives = 3
        isLevelFinished = false
        enemiesKilled = 0
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