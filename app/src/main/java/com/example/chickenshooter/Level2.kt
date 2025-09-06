package com.example.chickenshooter.levels

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import com.example.chickenshooter.*
import com.example.chickenshooter.utils.CollisionUtils

class Level2(
    context: Context,
    player: Player,
    bulletBitmap: Bitmap,
    itemBitmaps: List<Bitmap>,
    coinBmp: Bitmap,                            // đổi tên tham số để không trùng
    private val backgroundId: Int
) : BaseLevel(context, player, bulletBitmap, itemBitmaps, coinBmp) {

    private val background = BitmapFactory.decodeResource(context.resources, backgroundId)
    private val chickenBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.chicken2)
    private val eggBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.egg)

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
        chickenBitmap.width * 2 / 5,
        chickenBitmap.height * 2 / 5,
        true
    )

    // coin dùng bitmap từ tham số coinBmp (đã truyền vào BaseLevel)
    private val scaledCoinBitmap = Bitmap.createScaledBitmap(
        coinBmp,
        (chickenBitmap.width * 2 / 5) * 4 / 5,
        (chickenBitmap.height * 2 / 5) * 4 / 5,
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
        scaledCoinBitmap.width + 5,
        scaledCoinBitmap.height + 5,
        true
    )

    private val chickens = mutableListOf<Chicken>()
    private val items = mutableListOf<Item>()
    private val eggs = mutableListOf<Egg>()       // trứng boss
    private var boss: BossChicken? = null
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
        if (isLevelFinished || lives <= 0) return

        levelTimer++

        // Spawn boss sau khi hết thời gian
        if (!isBossSpawned && levelTimer >= levelDuration) {
            boss = BossChicken(
                x = (context.resources.displayMetrics.widthPixels - bossScaledBitmap.width) / 2,
                y = 50,
                bitmap = bossScaledBitmap,
                hp = 200,
                speed = 4,
                eggBitmap = eggBitmap
            )
            isBossSpawned = true
        }

        // Spawn gà thường khi chưa có boss
        if (!isBossSpawned) {
            spawnCooldown++
            if (spawnCooldown >= spawnInterval) {
                val randomX = (0..(context.resources.displayMetrics.widthPixels - scaledChickenBitmap.width)).random()
                val moveType = (0..3).random()
                chickens.add(Chicken(randomX, 0, scaledChickenBitmap, chickenSpeed, moveType, chickenHp))
                spawnCooldown = 0
            }
        }

        chickens.forEach { it.update() }
        chickens.removeAll { it.y > context.resources.displayMetrics.heightPixels }

        items.forEach { it.update() }
        items.removeAll { it.y > context.resources.displayMetrics.heightPixels }

        // Bullet - Chicken
        val deadChickens = mutableListOf<Chicken>()
        val usedBullets = mutableListOf<Bullet>()
        for (chicken in chickens) {
            for (bullet in bullets) {
                if (CollisionUtils.isColliding(chicken.getRect(), bullet.getRect())) {
                    chicken.hp -= bullet.damage
                    usedBullets.add(bullet)
                    if (chicken.hp <= 0) {
                        deadChickens.add(chicken)
                        // 10% rơi item
                        if ((0..99).random() < 10) {
                            val itemType = (0..2).random()
                            items.add(Item(chicken.x, chicken.y, itemBitmaps[itemType], itemType))
                        }
                        // Rơi xu: dùng hệ xu của BaseLevel
                        spawnCoin(chicken.x, chicken.y, chicken.bitmap.width, chicken.bitmap.height)
                        // mặc định 1 xu
                    }
                }
            }
        }
        chickens.removeAll(deadChickens)
        bullets.removeAll(usedBullets)

        // Player - Chicken
        val collidedChicken = chickens.firstOrNull { CollisionUtils.isColliding(it.getRect(), player.getRect()) }
        if (collidedChicken != null) {
            lives--
            chickens.remove(collidedChicken)
        }

        // Player - Item (đổi súng)
        val collectedItems = items.filter { CollisionUtils.isColliding(it.getRect(), player.getRect()) }
        for (item in collectedItems) {
            pickedGunMode = when (item.type) {
                0 -> GunMode.FAST
                1 -> GunMode.TRIPLE_PARALLEL
                2 -> GunMode.TRIPLE_SPREAD
                else -> null
            }
        }
        items.removeAll(collectedItems)

        // Cập nhật & nhặt xu (gọi hàm mặc định của BaseLevel)
        updateCoins()

        // Boss logic
        boss?.let { b ->
            b.update(System.currentTimeMillis(), eggs)

            // Bullet - Boss
            val usedBulletsBoss = mutableListOf<Bullet>()
            for (bullet in bullets) {
                if (CollisionUtils.isColliding(b.getRect(), bullet.getRect())) {
                    b.hp -= bullet.damage
                    usedBulletsBoss.add(bullet)
                }
            }
            bullets.removeAll(usedBulletsBoss)

            // Player - Boss
            if (CollisionUtils.isColliding(b.getRect(), player.getRect())) {
                lives--
            }

            if (b.hp <= 0) {
                isLevelFinished = true
            }
        }

        // Eggs
        eggs.forEach { it.update() }
        eggs.removeAll { it.isOutOfScreen }

        // Egg - Player
        val hitEgg = eggs.firstOrNull { CollisionUtils.isColliding(it.getRect(), player.getRect()) }
        if (hitEgg != null) {
            lives--
            eggs.remove(hitEgg)
        }
    }

    override fun draw(canvas: Canvas, bullets: List<Bullet>) {
        canvas.drawBitmap(background, null, canvas.clipBounds, null)
        player.draw(canvas)
        chickens.forEach { it.draw(canvas) }
        bullets.forEach { it.draw(canvas) }
        items.forEach { it.draw(canvas) }

        // Vẽ xu từ BaseLevel
        drawCoins(canvas)

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
    }

    override fun getBackground(): Bitmap = background
    override fun getLives(): Int = lives
}
