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
class Level3(
    context: Context,
    player: Player,
    bulletBitmap: Bitmap,
    itemBitmaps: List<Bitmap>,
    coinBmp: Bitmap,
    private val backgroundId: Int
) : BaseLevel(context, player, bulletBitmap, itemBitmaps, coinBmp) {

    private val background = BitmapFactory.decodeResource(context.resources, backgroundId)
    private val chickenBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.chicken3)
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
    private val bossBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.boss_chicken3)
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

    internal val chickens = mutableListOf<Chicken>()
    var boss: BossChicken? = null
    private var isBossSpawned = false

    // --- WAVE LOGIC ---
    private val wavePatterns = listOf(MoveType.BOUNCE, MoveType.SINE, MoveType.ZIGZAG)
    private var waveIndex = 0
    private var spawningWave = false

    private val chickenSpeed = 5f
    private val chickenHp = 3
    private var lives = 3
    private var isLevelFinished = false

    private var levelTimer = 0
    private val levelDuration = 5 * 60
    private var waveTimer = 0
    private val waveInterval = 120 // số frame cho mỗi đợt (2 giây nếu 60fps)
    private var enemiesKilled = 0
    private val requiredKills = 50 // Boss spawns after killing 25 enemies

    override var pickedGunMode: GunMode? = null

    override fun update(bullets: MutableList<Bullet>) {
        if (isLevelFinished) return

        scrollBackground?.update()
        levelTimer++
        waveTimer++
        if (waveTimer >= waveInterval && !isBossSpawned) {
            val pattern = wavePatterns[waveIndex % wavePatterns.size]
            spawnWave(pattern)
            waveIndex++
            waveTimer = 0
        }
        // Spawn boss sau khi giết đủ số quái
        if (!isBossSpawned && enemiesKilled >= requiredKills) {
            boss = BossChicken(
                x = (context.resources.displayMetrics.widthPixels - bossScaledBitmap.width) / 2,
                y = 50,
                bitmap = bossScaledBitmap,
                hp = 200,
                vx = 6,
                vy = 3,
                eggBitmap = eggBitmap,
                screenWidth = context.resources.displayMetrics.widthPixels,
                screenHeight = context.resources.displayMetrics.heightPixels
            )
            isBossSpawned = true
        }

        // Update chickens
        val playerCenterX = player.x + player.getRect().width() / 2f
        val playerCenterY = player.y + player.getRect().height() / 2f
        chickens.forEach { chicken ->
            chicken.update(playerCenterX, playerCenterY)
        }

        // ... (tất cả logic xử lý items, bullets, collision, boss, ... giữ nguyên như trước) ...
        // --- Dưới đây chỉ là các phần còn lại giữ nguyên, đã rút gọn cho phần spawn gà theo đợt là chính ---

        // Bullet - Chicken collision
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

        // Player - Chicken collision
        handlePlayerChickenCollision(chickens, 
            onPlayerHit = { 
                lives--
                player.hit(playerExplosionFrames)
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
                lives--
                player.hit(playerExplosionFrames)
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

    /**
     * Spawn 1 đợt gà với pattern di chuyển được chỉ định
     */
    private fun spawnWave(moveType: MoveType) {
        val numChickens = 7
        val screenW = context.resources.displayMetrics.widthPixels
        val screenH = context.resources.displayMetrics.heightPixels
        val availableWidth = screenW - scaledChickenBitmap.width
        val spacing = if (numChickens > 1)
            availableWidth.toFloat() / (numChickens - 1)
        else 0f

        // Hàng ngang đơn giản cho Level3
        for (i in 0 until numChickens) {
            val x = i * spacing
            chickens.add(
                Chicken(
                    x = x,
                    y = 0f,
                    bitmap = scaledChickenBitmap,
                    speed = chickenSpeed,
                    moveType = moveType,
                    hp = chickenHp,
                    screenWidth = screenW,
                    screenHeight = screenH,
                    shootChance = 60
                )
            )
        }
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
        lives = 3
        isLevelFinished = false
        waveIndex = 0
        levelTimer = 0
        waveTimer = 0
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