package com.example.chickenshooter.levels

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import com.example.chickenshooter.*
import com.example.chickenshooter.utils.CollisionUtils
import com.example.chickenshooter.Coin

class Level4(
    context: Context,
    player: Player,
    bulletBitmap: Bitmap,
    itemBitmaps: List<Bitmap>,
    private val backgroundId: Int
) : BaseLevel(context, player, bulletBitmap, itemBitmaps) {

    private val background = BitmapFactory.decodeResource(context.resources, backgroundId)
    private val chickenBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.chicken4)
    private val coinBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.coin)
    private val eggBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.egg) // Thêm bitmap trứng

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
    private val scaledCoinBitmap = Bitmap.createScaledBitmap(
        coinBitmap,
        scaledChickenBitmap.width * 4 / 5,
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

    private val chickens = mutableListOf<Chicken>()
    private val items = mutableListOf<Item>()
    private val coins = mutableListOf<Coin>()
    private val eggs = mutableListOf<Egg>() // Quản lý trứng boss thả
    private var boss: BossChicken? = null
    private var isBossSpawned = false

    private var spawnCooldown = 0
    private val spawnInterval = 55
    private val chickenSpeed = 5
    private val chickenHp = 3
    private var lives = 3
    private var isLevelFinished = false

    // Đếm thời gian màn chơi (1 phút = 60 * 60 frame)
    private var levelTimer = 0
    private val levelDuration = 20 * 60

    override var pickedGunMode: GunMode? = null

    override fun update(bullets: MutableList<Bullet>) {
        if (isLevelFinished || lives <= 0) return

        levelTimer++

        // Spawn boss đúng sau 1 phút
        if (!isBossSpawned && levelTimer >= levelDuration) {
            boss = BossChicken(
                x = (context.resources.displayMetrics.widthPixels - bossScaledBitmap.width) / 2,
                y = 50,
                bitmap = bossScaledBitmap,
                hp = 200,
                speed = 4,
                eggBitmap = eggBitmap // thêm bitmap trứng
            )
            isBossSpawned = true
        }

        // Chỉ spawn quái thường khi chưa có boss
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

        // Update coins
        coins.forEach { it.update() }
        coins.removeAll { it.y > context.resources.displayMetrics.heightPixels || it.isCollected }

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
                        // 10% drop item
                        if ((0..99).random() < 10) {
                            val itemType = (0..2).random()
                            items.add(Item(chicken.x, chicken.y, itemBitmaps[itemType], itemType))
                        }
                        // Luôn drop coin khi chết (hoặc bạn có thể random)
                        coins.add(Coin(chicken.x, chicken.y, scaledCoinBitmap))
                    }
                }
            }
        }
        chickens.removeAll(deadChickens)
        bullets.removeAll(usedBullets)

        // Player - Chicken collision
        val collidedChicken = chickens.firstOrNull { CollisionUtils.isColliding(it.getRect(), player.getRect()) }
        if (collidedChicken != null) {
            lives--
            chickens.remove(collidedChicken)
        }

        // Player - Item collision (báo về pickedGunMode)
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

        // Player - Coin collision
        val collectedCoins = coins.filter { !it.isCollected && CollisionUtils.isColliding(it.getRect(), player.getRect()) }
        for (coin in collectedCoins) {
            coin.isCollected = true
            player.gold += 1 // giả sử player có biến gold, nếu chưa có hãy thêm vào class Player
        }

        // Boss logic
        boss?.let { b ->
            b.update(System.currentTimeMillis(), eggs) // truyền danh sách eggs cho boss bắn

            // Bullet - Boss collision
            val usedBulletsBoss = mutableListOf<Bullet>()
            for (bullet in bullets) {
                if (CollisionUtils.isColliding(b.getRect(), bullet.getRect())) {
                    b.hp -= bullet.damage
                    usedBulletsBoss.add(bullet)
                }
            }
            bullets.removeAll(usedBulletsBoss)

            // Player - Boss collision (mất mạng)
            if (CollisionUtils.isColliding(b.getRect(), player.getRect())) {
                lives--
            }

            // Boss chết thì qua màn
            if (b.hp <= 0) {
                isLevelFinished = true
            }
        }

        // Update eggs (trứng boss bắn)
        eggs.forEach { it.update() }
        eggs.removeAll { it.isOutOfScreen }

        // Egg - Player collision
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
        coins.forEach { it.draw(canvas) }
        eggs.forEach { it.draw(canvas) } // vẽ trứng boss bắn
        boss?.draw(canvas)
    }

    override fun isCompleted(): Boolean = isLevelFinished
    override fun reset() {
        chickens.clear()
        items.clear()
        coins.clear()
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