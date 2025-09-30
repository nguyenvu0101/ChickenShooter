package com.example.chickenshooter.levels

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import com.example.chickenshooter.*
import com.example.chickenshooter.utils.CollisionUtils
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import com.example.chickenshooter.R
import com.example.chickenshooter.utils.SpriteUtils
import kotlin.random.Random
import com.example.chickenshooter.ChickenSwarm

class Level2(
    context: Context,
    player: Player,
    bulletBitmap: Bitmap,
    itemBitmaps: List<Bitmap>,
    coinBmp: Bitmap,                              // đổi tên tham số để không trùng
    private val backgroundId: Int
): BaseLevel(context, player, bulletBitmap, itemBitmaps ,  coinBmp) {

    private val background = BitmapFactory.decodeResource(context.resources, backgroundId)
    private val chickenBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.chicken2)
    private val eggBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.egg)
    private val shields = mutableListOf<Shield>()
    private val healthItems = mutableListOf<HealthItem>()
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
        R.drawable.explosion_animation_v1, // sprite sheet nổ của player
        rows = 8,
        cols = 8 // số frame trong sheet
    )


    private val bossExplosionFrames = listOf(
        BitmapFactory.decodeResource(context.resources, R.drawable.playership1_damage1),
        BitmapFactory.decodeResource(context.resources, R.drawable.playership1_damage2),
        BitmapFactory.decodeResource(context.resources, R.drawable.playership1_damage3),
        BitmapFactory.decodeResource(context.resources, R.drawable.playership2_damage1),
        BitmapFactory.decodeResource(context.resources, R.drawable.playership2_damage2),
        BitmapFactory.decodeResource(context.resources, R.drawable.playership2_damage3),
        BitmapFactory.decodeResource(context.resources, R.drawable.playership3_damage1),
        BitmapFactory.decodeResource(context.resources, R.drawable.playership3_damage2),
        BitmapFactory.decodeResource(context.resources, R.drawable.playership3_damage3)
    )
    private val explosions = mutableListOf<Explosion>()
    private var bossExplosionStarted = false

    private val scaledChickenBitmap = Bitmap.createScaledBitmap(
        chickenBitmap,
        chickenBitmap.width / 13,
        chickenBitmap.height / 13,
        true
    )
    private val manaBitmap = Bitmap.createScaledBitmap(
        BitmapFactory.decodeResource(context.resources, R.drawable.mana),
        scaledChickenBitmap.width, // cùng width với gà
        scaledChickenBitmap.height, // cùng height với gà
        true
    )
    // coin dùng bitmap từ tham số coinBmp (đã truyền vào BaseLevel)
// coin gần bằng quái nhưng nhỏ hơn ~1/3
    override val coinBitmap: Bitmap = Bitmap.createScaledBitmap(
        coinBmp, // dùng bitmap gốc truyền vào
        scaledChickenBitmap.width * 4 / 5, // scale theo ý muốn
        scaledChickenBitmap.height * 4 / 5,
        true
    )
    private val bossBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.boss_chicken)
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

    internal val chickens = mutableListOf<Chicken>() // gà
    val swarms = mutableListOf<ChickenSwarm>()   // Đàn gà

    private val items = mutableListOf<Item>()
    private val eggs = mutableListOf<Egg>()       // trứng boss
    var boss: BossChicken? = null
    private var isBossSpawned = false

    private var spawnCooldown = 0
    private val spawnInterval = 55
    private val chickenSpeed = 5
    private val chickenHp = 3
    private var lives = 3
    private var isLevelFinished = false

    private var levelTimer = 0
    private val levelDuration = 20 * 60

    override var pickedGunMode: GunMode? = null

    override fun update(bullets: MutableList<Bullet>) {
        if (isLevelFinished) return

        scrollBackground?.update()
        levelTimer++

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

        // Spawn gà thường khi chưa có boss
        if (!isBossSpawned) {
            spawnCooldown++
            if (spawnCooldown >= spawnInterval) {
                val randomX = (0..(context.resources.displayMetrics.widthPixels - scaledChickenBitmap.width)).random()
                // Mở rộng moveType để sử dụng các pattern mới (0-8)
                val moveType = (0..8).random()
                val chickenSpeed = Random.nextFloat() * 3f + 2f // từ 2.0 đến 5.0

                chickens.add(Chicken(
                    x = randomX.toFloat(),
                    y = 0f,
                    bitmap = scaledChickenBitmap,
                    speed = chickenSpeed,
                    moveType = moveType,
                    hp = chickenHp,
                    screenWidth = context.resources.displayMetrics.widthPixels,
                    screenHeight = context.resources.displayMetrics.heightPixels
                ))
                spawnCooldown = 0
            }
        }

        // --- THÊM MỚI: thỉnh thoảng spawn đàn gà ---
        if (!isBossSpawned){
            if (Random.nextInt(0, 600) == 0) {   // 1/400 frame
                val swarm = ChickenSwarm(
                    context = context,
                    numChickens = 6,
                    chickenBitmap = scaledChickenBitmap,
                    screenWidth = context.resources.displayMetrics.widthPixels,
                    screenHeight = context.resources.displayMetrics.heightPixels
                )
                swarms.add(swarm)
            }
        }

        // Update chickens và pass player position cho AI targeting
        val playerCenterX = player.x + player.getRect().width() / 2f
        val playerCenterY = player.y + player.getRect().height() / 2f

        chickens.forEach { chicken ->
            chicken.update(playerCenterX, playerCenterY)
        }
        chickens.removeAll { it.isOffScreen() }

        // Update đàn gà
        swarms.forEach { swarm -> swarm.update(playerCenterX, playerCenterY) }
        swarms.removeAll { it.isEmpty() }

        items.forEach { it.update() }
        items.removeAll { it.y > context.resources.displayMetrics.heightPixels }

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
                        // Drop shields
                        if ((0..99).random() < 10) {
                            shields.add(Shield(chicken.x.toInt(), chicken.y.toInt(), scaledShieldBitmap, 5))
                        }
                        // Drop health items
                        if ((0..99).random() < 8) {
                            healthItems.add(HealthItem(chicken.x.toInt(), chicken.y.toInt(), scaledHealthItemBitmap, 5))
                        }
                        // Drop items
                        if ((0..99).random() < 10) {
                            val itemType = (0..2).random()
                            items.add(Item(chicken.x.toInt(), chicken.y.toInt(), itemBitmaps[itemType], ItemType.values()[itemType], 12))
                        }
                        // Drop mana
                        if (Math.random() < 0.10) {
                            spawnMana(chicken.x.toInt(), chicken.y.toInt(), manaBitmap, 8)
                        }
                        // Drop coins
                        spawnCoin(chicken.x.toInt(), chicken.y.toInt(), chicken.bitmap.width, chicken.bitmap.height)
                    }
                }
            }
        }

// Với đàn gà (swarms)
        for (swarm in swarms) {
            val deadInSwarm = mutableListOf<Chicken>()
            val usedBullets = mutableListOf<Bullet>()

            for (chicken in swarm.chickens) {
                for (bullet in bullets) {
                    if (CollisionUtils.isColliding(chicken.getRect(), bullet.getRect())) {
                        chicken.hp -= bullet.damage
                        usedBullets.add(bullet)
                        if (chicken.hp <= 0) {
                            deadInSwarm.add(chicken)
                        }
                    }
                }
            }

            if (deadInSwarm.isNotEmpty()) {
                // Gọi dropLoot thông qua swarm
                swarm.dropLoot(
                    deadChickens = deadInSwarm,
                    items = items,
                    shields = shields,
                    healthItems = healthItems,
                    manaBitmap = manaBitmap,
                    itemBitmaps = itemBitmaps,
                    scaledShieldBitmap = scaledShieldBitmap,
                    scaledHealthItemBitmap = scaledHealthItemBitmap,
                    spawnCoin = ::spawnCoin
                )

                swarm.chickens.removeAll(deadInSwarm)
            }

            bullets.removeAll(usedBullets)
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

        // --- Xử lý chicken projectiles (gộp cả swarm) <<< ---
        val allChickenProjectiles = mutableListOf<ChickenProjectile>()
        chickens.forEach { allChickenProjectiles.addAll(it.projectiles) }
        swarms.forEach { swarm -> swarm.chickens.forEach { allChickenProjectiles.addAll(it.projectiles) } }

        val hitProjectiles = mutableListOf<ChickenProjectile>()
        allChickenProjectiles.forEach { projectile ->
            if (CollisionUtils.isColliding(projectile.getRect(), player.getRect())) {
                if (!player.hasShield) {
                    lives--
                    player.hit(playerExplosionFrames)
                }
                hitProjectiles.add(projectile)
            }
        }
        chickens.forEach { it.projectiles.removeAll(hitProjectiles) }
        swarms.forEach { it.chickens.forEach { ch -> ch.projectiles.removeAll(hitProjectiles) } }

        // Bullet - Chicken Projectiles collision (optional: bullets có thể phá được projectiles)
        val destroyedProjectiles = mutableListOf<ChickenProjectile>()
        val usedBulletsForProjectiles = mutableListOf<Bullet>()

        chickens.forEach { chicken ->
            chicken.projectiles.forEach { projectile ->
                bullets.forEach { bullet ->
                    if (CollisionUtils.isColliding(projectile.getRect(), bullet.getRect())) {
                        destroyedProjectiles.add(projectile)
                        usedBulletsForProjectiles.add(bullet)
                    }
                }
            }
        }

        // Remove destroyed projectiles and used bullets
        chickens.forEach { chicken ->
            chicken.projectiles.removeAll(destroyedProjectiles)
        }
        bullets.removeAll(usedBulletsForProjectiles)

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
        updateCoins()
        updateMana()
        shields.forEach { it.update() }
        shields.removeAll { it.y > context.resources.displayMetrics.heightPixels }
        healthItems.forEach { it.update() }
        healthItems.removeAll { it.y > context.resources.displayMetrics.heightPixels }

        // Collect shields
        val collectedShields = shields.filter { CollisionUtils.isColliding(it.getRect(), player.getRect()) }
        for (shield in collectedShields) {
            player.activateShield(6000) // Shield protection for 6 seconds
        }
        shields.removeAll(collectedShields)

        // Collect health items
        val collectedHealthItems = healthItems.filter { CollisionUtils.isColliding(it.getRect(), player.getRect()) }
        for (healthItem in collectedHealthItems) {
            if (lives < 3) { // Only heal if not at max lives
                lives++
            }
        }
        healthItems.removeAll(collectedHealthItems)

        // Boss logic
        boss?.let { b ->
            b.update(System.currentTimeMillis(), eggs)

            // Bullet - Boss collision
            val usedBulletsBoss = mutableListOf<Bullet>()
            for (bullet in bullets) {
                if (CollisionUtils.isColliding(b.getRect(), bullet.getRect())) {
                    b.hp -= bullet.damage
                    usedBulletsBoss.add(bullet)
                }
            }
            bullets.removeAll(usedBulletsBoss)

            // Player - Boss collision
            if (CollisionUtils.isColliding(b.getRect(), player.getRect())) {
                if (!player.hasShield) {
                    lives--
                    player.hit(playerExplosionFrames)
                }
            }

            if (b.hp <= 0) {
                isLevelFinished = true
                onBossDefeated?.invoke() // Gọi callback khi boss bị đánh bại
            }
        }

        // Boss eggs update
        eggs.forEach { it.update() }
        eggs.removeAll { it.isOutOfScreen }

        // Egg - Player collision
        val hitEgg = eggs.firstOrNull { CollisionUtils.isColliding(it.getRect(), player.getRect()) }
        if (hitEgg != null) {
            if (!player.hasShield) {
                lives--
                player.hit(playerExplosionFrames)
            }
            eggs.remove(hitEgg)
        }

        // Check game over
        if (lives <= 0) {
            isLevelFinished = true
        }

        // Update player
        player.update()
    }

    override fun draw(canvas: Canvas, bullets: List<Bullet>) {
        scrollBackground?.draw(canvas)

        player.draw(canvas)
        chickens.forEach { it.draw(canvas) }

        // Vẽ đàn gà
        swarms.forEach { it.draw(canvas) }

        bullets.forEach { it.draw(canvas) }
        items.forEach { it.draw(canvas) }
        shields.forEach { it.draw(canvas) }
        healthItems.forEach { it.draw(canvas) }
        player.draw(canvas)
        // Vẽ xu từ BaseLevel
        drawCoins(canvas)
        // Vẽ bình mana:
        drawMana(canvas)
        eggs.forEach { it.draw(canvas) }
        boss?.draw(canvas)
        boss?.let { b ->
            b.draw(canvas)

            // Vẽ thanh máu boss
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

            // Vẽ nền thanh máu
            canvas.drawRect(left.toFloat(), top.toFloat(), (left + barWidth).toFloat(), (top + barHeight).toFloat(), paintBg)
            // Vẽ phần máu còn lại
            canvas.drawRect(left.toFloat(), top.toFloat(), (left + hpBarCurrentWidth).toFloat(), (top + barHeight).toFloat(), paintHp)
            // Vẽ viền trắng
            canvas.drawRect(left.toFloat(), top.toFloat(), (left + barWidth).toFloat(), (top + barHeight).toFloat(), paintBorder)
            // Vẽ số máu
            canvas.drawText("Boss HP: ${b.hp}/${b.maxHp}", canvas.width / 2f, (top + barHeight + 32).toFloat(), paintText)
        }
    }

    override fun isCompleted(): Boolean = isLevelFinished

    override fun reset() {
        chickens.clear()
        swarms.clear() // đàn gà
        items.clear()
        // coins do BaseLevel quản lý, BaseLevel.reset() của bạn không xóa -> không sao,
        // nếu muốn sạch tuyệt đối có thể thêm hàm clearCoins() trong BaseLevel.
        eggs.clear()
        boss = null
        isBossSpawned = false
        lives = 3
        isLevelFinished = false
        spawnCooldown = 0
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
            android.util.Log.d("Level2", "Cleaning up Level2 specific resources...")
            
            // Clear all collections
            chickens.clear()
            swarms.clear()
            items.clear()
            eggs.clear()
            
            // Clear boss
            boss = null
            
            android.util.Log.d("Level2", "Level2 cleanup completed")
        } catch (e: Exception) {
            android.util.Log.e("Level2", "Error during Level2 cleanup: ${e.message}")
            e.printStackTrace()
        }
    }
}
