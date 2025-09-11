package com.example.chickenshooter.levels

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import com.example.chickenshooter.*
import com.example.chickenshooter.utils.CollisionUtils
import android.content.Intent
import com.example.chickenshooter.utils.SpriteUtils

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
    private val shieldBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.shield_item)
    private val scaledShieldBitmap = Bitmap.createScaledBitmap(
        shieldBitmap,
        (shieldBitmap.width * 0.07).toInt(),
        (shieldBitmap.height * 0.07).toInt(),
        true
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
        bossBitmap.width * 3 / 5,
        bossBitmap.height * 3 / 5,
        true
    )

    private val scaledEggBitmap = Bitmap.createScaledBitmap(
        eggBitmap,
        coinBitmap.width + 5,
        coinBitmap.height + 5,
        true
    )

    internal val chickens = mutableListOf<Chicken>()
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

    private val playerExplosionFrames = SpriteUtils.splitSpriteSheet(
        context,
        R.drawable.explosion_animation_v1, // sprite sheet nổ của player
        rows = 8,
        cols = 8 // số frame trong sheet
    )

    override fun update(bullets: MutableList<Bullet>) {
        if (isLevelFinished) return

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
                // Sử dụng moveType từ 0-8 cho các pattern mới
                val moveType = (0..8).random()
                // Tạo speed ngẫu nhiên
                val randomSpeed = kotlin.random.Random.nextFloat() * 3f + 2f // 2f đến 5f

                chickens.add(Chicken(
                    x = randomX.toFloat(), // Chuyển sang Float
                    y = 0f, // Chuyển sang Float
                    bitmap = scaledChickenBitmap,
                    speed = randomSpeed, // Sử dụng random speed thay vì chickenSpeed cố định
                    moveType = moveType,
                    hp = chickenHp,
                    screenWidth = context.resources.displayMetrics.widthPixels,
                    screenHeight = context.resources.displayMetrics.heightPixels
                ))
                spawnCooldown = 0
            }
        }

        // Update chickens với player position để AI có thể target
        val playerCenterX = player.x + player.getRect().width() / 2f
        val playerCenterY = player.y + player.getRect().height() / 2f

        chickens.forEach { chicken ->
            chicken.update(playerCenterX, playerCenterY) // Pass player position
        }
        chickens.removeAll { it.isOffScreen() } // Sử dụng method mới

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
                        // Drop shields - convert Float to Int
                        if ((0..99).random() < 30) {
                            shields.add(Shield(chicken.x.toInt(), chicken.y.toInt(), scaledShieldBitmap, 5))
                        }
                        // Drop items - convert Float to Int
                        if ((0..99).random() < 10) {
                            val itemType = (0..2).random()
                            items.add(Item(chicken.x.toInt(), chicken.y.toInt(), itemBitmaps[itemType], ItemType.values()[itemType], 12))
                        }
                        // Drop mana - convert Float to Int
                        if (Math.random() < 0.35) {
                            spawnMana(chicken.x.toInt(), chicken.y.toInt(), manaBitmap, 8)
                        }
                        // Drop coins - convert Float to Int
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
                player.hit(playerExplosionFrames) // Thêm hit effect
            }
            chickens.remove(collidedChicken)
        }

        // *** MỚI: Xử lý chicken projectiles ***
        // Player - Chicken Projectiles collision
        val hitProjectiles = mutableListOf<ChickenProjectile>()
        chickens.forEach { chicken ->
            chicken.projectiles.forEach { projectile ->
                if (CollisionUtils.isColliding(projectile.getRect(), player.getRect())) {
                    if (!player.hasShield) {
                        lives--
                        player.hit(playerExplosionFrames)
                    }
                    hitProjectiles.add(projectile)
                }
            }
        }

        // Remove hit projectiles
        chickens.forEach { chicken ->
            chicken.projectiles.removeAll(hitProjectiles)
        }

        // Optional: Bullet có thể bắn phá chicken projectiles
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

        // Update coins, mana, shields
        updateCoins()
        updateMana()
        shields.forEach { it.update() }
        shields.removeAll { it.y > context.resources.displayMetrics.heightPixels }

        // Collect shields
        val collectedShields = shields.filter { CollisionUtils.isColliding(it.getRect(), player.getRect()) }
        for (shield in collectedShields) {
            player.activateShield(6000)
        }
        shields.removeAll(collectedShields)

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
                    player.hit(playerExplosionFrames) // Thêm hit effect
                }
            }

            if (b.hp <= 0) {
                isLevelFinished = true
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
                player.hit(playerExplosionFrames) // Thêm hit effect
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
        canvas.drawBitmap(background, null, canvas.clipBounds, null)
        player.draw(canvas)
        chickens.forEach { it.draw(canvas) }
        bullets.forEach { it.draw(canvas) }
        items.forEach { it.draw(canvas) }
        shields.forEach { it.draw(canvas) }
        player.draw(canvas)
        // Vẽ xu từ BaseLevel
        drawCoins(canvas)
// Vẽ bình mana:
        drawMana(canvas)
        eggs.forEach { it.draw(canvas) }
        boss?.draw(canvas)
    }

    override fun isCompleted(): Boolean = isLevelFinished

    override fun reset() {
        chickens.clear()
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
}
