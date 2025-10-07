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
    private val allProjectiles = mutableListOf<ChickenProjectile>()
    internal val chickens = mutableListOf<Chicken>()
    private val shields = mutableListOf<Shield>()
    private val healthItems = mutableListOf<HealthItem>()
    private val items = mutableListOf<Item>()
    private val eggs = mutableListOf<Egg>()
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
    private val levelDuration = 1 * 60
    private var waveTimer = 0
    private val waveInterval = 120 // số frame cho mỗi đợt (3 giây nếu 60fps)
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
        // Spawn boss sau khi hết thời gian
        if (!isBossSpawned && levelTimer >= levelDuration) {
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

        // --- SPAWN GÀ THEO ĐỢT, LẶP LẠI ---
//        // Spawn wave mới khi hết quái, cho đến khi boss ra
//        if (!isBossSpawned && chickens.isEmpty()) {
//            val pattern = wavePatterns[waveIndex % wavePatterns.size]
//            spawnWave(pattern)
//            waveIndex++
//        }


        // Update chickens
        val playerCenterX = player.x + player.getRect().width() / 2f
        val playerCenterY = player.y + player.getRect().height() / 2f
        chickens.forEach { chicken ->
            chicken.update(playerCenterX, playerCenterY)
        }
// Kiểm tra va chạm giữa ChickenProjectile và Player
        for (chicken in chickens) {
            chicken.update(playerCenterX, playerCenterY)
            // Chuyển tất cả projectiles của gà này sang allProjectiles
            allProjectiles.addAll(chicken.projectiles)
            chicken.projectiles.clear() // Xóa trong gà để không bị add lại ở frame sau
        }
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

        val it = allProjectiles.iterator()
        while (it.hasNext()) {
            val proj = it.next()
            proj.update()
            if (proj.y > context.resources.displayMetrics.heightPixels) {
                it.remove()
            }
        }
        // Bullet - Chicken collision
        val deadChickens = mutableListOf<Chicken>()
        val usedBullets = mutableListOf<Bullet>()
        for (chicken in chickens) {
            for (bullet in bullets) {
                if (CollisionUtils.isColliding(chicken.getRect(), bullet.getRect())) {
                    chicken.hp -= bullet.damage
                    usedBullets.add(bullet)
                    if (chicken.hp <= 0) {
                        deadChickens.add(chicken)
                        if ((0..99).random() < 3) shields.add(Shield(chicken.x.toInt(), chicken.y.toInt(), scaledShieldBitmap, 5))
                        if ((0..99).random() < 3) healthItems.add(HealthItem(chicken.x.toInt(), chicken.y.toInt(), scaledHealthItemBitmap, 5))
                        if ((0..99).random() < 3) {
                            val itemType = (0..2).random()
                            items.add(Item(chicken.x.toInt(), chicken.y.toInt(), itemBitmaps[itemType], ItemType.values()[itemType], 12))
                        }
                        if (Math.random() < 0.03) spawnMana(chicken.x.toInt(), chicken.y.toInt(), manaBitmap, 8)
                        spawnCoin(chicken.x.toInt(), chicken.y.toInt(), chicken.bitmap.width, chicken.bitmap.height)
                    }
                }
            }
        }
        chickens.removeAll(deadChickens)
        bullets.removeAll(usedBullets)

        // Player - Chicken collision
        val collidedChicken = chickens.firstOrNull { CollisionUtils.isColliding(it.getRect(), player.getRect()) }
        if (collidedChicken != null) {
            if (!player.hasShield) {
                lives--
                player.hit(playerExplosionFrames)
            }
            chickens.remove(collidedChicken)
        }

        // Player - Item collection
        val collectedItems = items.filter { CollisionUtils.isColliding(it.getRect(), player.getRect()) }
        for (item in collectedItems) {
            pickedGunMode = when (item.type.ordinal) {
                0 -> GunMode.FAST
                1 -> GunMode.TRIPLE_PARALLEL
                2 -> GunMode.TRIPLE_SPREAD
                else -> null
            }
        }
        items.removeAll(collectedItems)

        // Update coins, mana, shields, health items
        // Update coins, mana, shields, health items, and gun items
        updateCoins()
        updateMana()

        items.forEach { it.update() }   // 🟢 thêm dòng này
        items.removeAll { it.y > context.resources.displayMetrics.heightPixels }

        shields.forEach { it.update() }
        shields.removeAll { it.y > context.resources.displayMetrics.heightPixels }

        healthItems.forEach { it.update() }
        healthItems.removeAll { it.y > context.resources.displayMetrics.heightPixels }


        // Collect shields
        val collectedShields = shields.filter { CollisionUtils.isColliding(it.getRect(), player.getRect()) }
        for (shield in collectedShields) {
            player.activateShield(6000)
        }
        shields.removeAll(collectedShields)

        // Collect health items
        val collectedHealthItems = healthItems.filter { CollisionUtils.isColliding(it.getRect(), player.getRect()) }
        for (healthItem in collectedHealthItems) {
            if (lives < 3) lives++
        }
        healthItems.removeAll(collectedHealthItems)

        // Boss logic
        boss?.let { b ->
            b.update(System.currentTimeMillis(), eggs)
            val usedBulletsBoss = mutableListOf<Bullet>()
            for (bullet in bullets) {
                if (CollisionUtils.isColliding(b.getRect(), bullet.getRect())) {
                    b.hp -= bullet.damage
                    usedBulletsBoss.add(bullet)
                }
            }
            bullets.removeAll(usedBulletsBoss)
            if (CollisionUtils.isColliding(b.getRect(), player.getRect())) {
                if (!player.hasShield) {
                    lives--
                    player.hit(playerExplosionFrames)
                }
            }
            if (b.hp <= 0) {
                isLevelFinished = true
                try {
                    onBossDefeated?.invoke()
                } catch (_: Exception) {}
            }
        }

        eggs.forEach { it.update() }
        eggs.removeAll { it.isOutOfScreen }
        val hitEgg = eggs.firstOrNull { CollisionUtils.isColliding(it.getRect(), player.getRect()) }
        if (hitEgg != null) {
            if (!player.hasShield) {
                lives--
                player.hit(playerExplosionFrames)
            }
            eggs.remove(hitEgg)
        }

        if (lives <= 0) isLevelFinished = true

        player.update()
    }

    /**
     * Spawn 1 đợt gà: kiểu DOWN, SINE, ZIGZAG, lặp lại theo wavePatterns
     */
    private fun spawnWave(moveType: MoveType) {
        val numChickens = 8
        val screenW = context.resources.displayMetrics.widthPixels
        val screenH = context.resources.displayMetrics.heightPixels
        val availableWidth = screenW - scaledChickenBitmap.width
        val spacing = if (numChickens > 1)
            availableWidth.toFloat() / (numChickens - 1)
        else 0f

        // Chọn đội hình dựa trên waveIndex (hoặc random nếu thích)
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
                            speed = chickenSpeed,
                            moveType = moveType,
                            hp = chickenHp,
                            screenWidth = screenW,
                            screenHeight = screenH
                        )
                    )
                }
            }
            1 -> { // HÌNH CHỮ V NHỌN, ĐẦY ĐỦ 2 GÓC
                val mid = (numChickens - 1) / 2f
                val vHeight = 200f // Độ cao chữ V, tăng số này cho V cao nhọn hơn
                for (i in 0 until numChickens) {
                    val x = i * spacing // Trải đều từ trái sang phải, luôn đủ 2 bên
                    val dx = abs(i - mid)
                    // Tỉ lệ nhọn: càng gần giữa càng cao, càng xa càng thấp
                    val y = dx / mid * vHeight // Góc ngoài cùng y=Vmax, giữa y=0
                    chickens.add(
                        Chicken(
                            x = x,
                            y = y,
                            bitmap = scaledChickenBitmap,
                            speed = chickenSpeed,
                            moveType = moveType,
                            hp = chickenHp,
                            screenWidth = screenW,
                            screenHeight = screenH
                        )
                    )
                }
            }
            2 -> { // DẠNG ZIGZAG
                for (i in 0 until numChickens) {
                    val x = i * spacing
                    val y = if (i % 2 == 0) 0f else 50f
                    chickens.add(
                        Chicken(
                            x = x,
                            y = y,
                            bitmap = scaledChickenBitmap,
                            speed = chickenSpeed,
                            moveType = moveType,
                            hp = chickenHp,
                            screenWidth = screenW,
                            screenHeight = screenH
                        )
                    )
                }
            }
            3 -> { // HÌNH SIN ĐẦU (gà xuất hiện như sóng sin)
                val amplitude = 60f
                val freq = Math.PI / numChickens
                for (i in 0 until numChickens) {
                    val x = i * spacing
                    val y = amplitude * (1 + Math.sin(i * freq)).toFloat()
                    chickens.add(
                        Chicken(
                            x = x,
                            y = y,
                            bitmap = scaledChickenBitmap,
                            speed = chickenSpeed,
                            moveType = moveType,
                            hp = chickenHp,
                            screenWidth = screenW,
                            screenHeight = screenH
                        )
                    )
                }
            }
        }
    }

    override fun draw(canvas: Canvas, bullets: List<Bullet>, backgroundY: Float) {
        scrollBackground?.draw(canvas)
        player.draw(canvas)
        chickens.forEach { it.draw(canvas) }
        bullets.forEach { it.draw(canvas) }
        items.forEach { it.draw(canvas) }
        shields.forEach { it.draw(canvas) }
        healthItems.forEach { it.draw(canvas) }
        player.draw(canvas)
        drawCoins(canvas)
        drawMana(canvas)
        eggs.forEach { it.draw(canvas) }
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
        chickens.clear()
        items.clear()
        eggs.clear()
        coins.clear()
        shields.clear()
        boss = null
        isBossSpawned = false
        lives = 3
        isLevelFinished = false
        waveIndex = 0
        levelTimer = 0
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
            items.clear()
            eggs.clear()
            boss = null
        } catch (_: Exception) {}
    }
}