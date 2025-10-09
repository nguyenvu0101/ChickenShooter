package com.example.chickenshooter.levels

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.Paint
import android.graphics.Color
import com.example.chickenshooter.*
import com.example.chickenshooter.utils.CollisionUtils

abstract class BaseLevel(
    val context: Context,
    val player: Player,
    val bulletBitmap: Bitmap,
    val itemBitmaps: List<Bitmap>,
    open val coinBitmap: Bitmap
) {
    // --- Gun mode ---
    open var pickedGunMode: GunMode? = null
    open fun canUseMissile(): Boolean = false
    open fun consumeManaForMissile() {}
    
    // --- Boss defeat callback ---
    var onBossDefeated: (() -> Unit)? = null
    // --- Coins system ---
    protected val coins = mutableListOf<Coin>()
    var onCoinCollected: ((amount: Int) -> Unit)? = null
    private var scrW = 0
    private var scrH = 0

    protected var scrollBackground : Background? = null

    fun setScreenSize(w: Int, h: Int) {
        scrW = w
        scrH = h
        scrollBackground  = Background(getBackground(), w, h, speed = 8)
    }


    /** Gọi khi địch chết để thả xu tại (x,y) */
    open fun spawnCoin(enemyX: Int, enemyY: Int, enemyW: Int, enemyH: Int) {
        val coinX = enemyX + enemyW / 2 - coinBitmap.width / 2
        val coinY = enemyY + enemyH / 2 - coinBitmap.height / 2
        coins.add(Coin(coinX, coinY, coinBitmap))
    }

    /** Cập nhật rơi và nhặt xu */
    open fun updateCoins() {
        val it = coins.iterator()
        val playerRect: Rect = player.getRect()
        while (it.hasNext()) {
            val c = it.next()
            c.update()
            if (!c.isCollected && Rect.intersects(playerRect, c.getRect())) {
                c.isCollected = true
                onCoinCollected?.invoke(1)
                it.remove()
            } else if (scrH > 0 && c.y > scrH) {
                it.remove()
            }
        }
    }

    /** Lưu xu về hệ thống (chỉ offline) */
    fun saveCoinsToSystem() {
        val coinsEarned = coins.size.toLong()
        val prefs = context.getSharedPreferences("game", Context.MODE_PRIVATE)
        val currentCoins = prefs.getLong("coins", 0L)
        prefs.edit().putLong("coins", currentCoins + coinsEarned).apply()
    }

    /** Vẽ xu */
    open fun drawCoins(canvas: Canvas) {
        coins.forEach { it.draw(canvas) }
    }

    // ----------- Mana System for Missile ------------
    var manaCount = 0
    val manaNeededForMissile = 2
    protected val manaItems = mutableListOf<Item>()
    var onManaCollected: ((amount: Int) -> Unit)? = null

    // ----------- Shield System ------------
    protected val shields = mutableListOf<Shield>()
    private var shieldBitmap: Bitmap? = null
    
    fun setShieldBitmap(bitmap: Bitmap) {
        shieldBitmap = bitmap
    }
    
    open fun spawnShield(x: Int, y: Int) {
        shieldBitmap?.let { bitmap ->
            shields.add(Shield(x, y, bitmap, 5))
        }
    }
    
    open fun updateShields() {
        shields.forEach { it.update() }
        shields.removeAll { it.y > scrH }
        
        // Collect shields
        val collectedShields = shields.filter { CollisionUtils.isColliding(it.getRect(), player.getRect()) }
        for (shield in collectedShields) {
            player.activateShield(6000)
        }
        shields.removeAll(collectedShields)
    }
    
    open fun drawShields(canvas: Canvas) {
        shields.forEach { it.draw(canvas) }
    }

    // ----------- Health Items System ------------
    protected val healthItems = mutableListOf<HealthItem>()
    private var healthItemBitmap: Bitmap? = null
    
    fun setHealthItemBitmap(bitmap: Bitmap) {
        healthItemBitmap = bitmap
    }
    
    open fun spawnHealthItem(x: Int, y: Int) {
        healthItemBitmap?.let { bitmap ->
            healthItems.add(HealthItem(x, y, bitmap, 5))
        }
    }
    
    open fun updateHealthItems() {
        healthItems.forEach { it.update() }
        healthItems.removeAll { it.y > scrH }
        
        // Collect health items
        val collectedHealthItems = healthItems.filter { CollisionUtils.isColliding(it.getRect(), player.getRect()) }
        for (healthItem in collectedHealthItems) {
            if (getLives() < 3) {
                // This will be handled by the level-specific lives system
                onHealthCollected?.invoke()
            }
        }
        healthItems.removeAll(collectedHealthItems)
    }
    
    open fun drawHealthItems(canvas: Canvas) {
        healthItems.forEach { it.draw(canvas) }
    }
    
    var onHealthCollected: (() -> Unit)? = null

    // ----------- Gun Items System ------------
    protected val items = mutableListOf<Item>()
    
    open fun spawnGunItem(x: Int, y: Int, itemType: ItemType) {
        val itemBitmap = when (itemType) {
            ItemType.FAST -> itemBitmaps[0]
            ItemType.PARALLEL -> itemBitmaps[1]
            ItemType.SPREAD -> itemBitmaps[2]
            else -> itemBitmaps[0]
        }
        items.add(Item(x, y, itemBitmap, itemType, 12))
    }
    
    open fun updateGunItems() {
        items.forEach { it.update() }
        items.removeAll { it.y > scrH }
        
        // Collect gun items
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
    }
    
    open fun drawGunItems(canvas: Canvas) {
        items.forEach { it.draw(canvas) }
    }

    // ----------- Egg System ------------
    protected val eggs = mutableListOf<Egg>()
    
    open fun updateEggs() {
        eggs.forEach { it.update() }
        eggs.removeAll { it.isOutOfScreen }
        
        // Check egg collision with player
        val hitEgg = eggs.firstOrNull { CollisionUtils.isColliding(it.getRect(), player.getRect()) }
        if (hitEgg != null) {
            if (!player.hasShield) {
                onEggHit?.invoke()
            }
            eggs.remove(hitEgg)
        }
    }
    
    open fun drawEggs(canvas: Canvas) {
        eggs.forEach { it.draw(canvas) }
    }
    
    var onEggHit: (() -> Unit)? = null

    // ----------- Collision Logic ------------
    open fun handleBulletChickenCollision(chickens: MutableList<Chicken>, bullets: MutableList<Bullet>, onEnemyKilled: (Chicken) -> Unit) {
        val deadChickens = mutableListOf<Chicken>()
        val usedBullets = mutableListOf<Bullet>()
        
        for (chicken in chickens) {
            for (bullet in bullets) {
                if (CollisionUtils.isColliding(chicken.getRect(), bullet.getRect())) {
                    chicken.hp -= bullet.damage
                    usedBullets.add(bullet)
                    if (chicken.hp <= 0) {
                        deadChickens.add(chicken)
                        onEnemyKilled(chicken)
                    }
                }
            }
        }
        chickens.removeAll(deadChickens)
        bullets.removeAll(usedBullets)
    }
    
    open fun handlePlayerChickenCollision(chickens: MutableList<Chicken>, onPlayerHit: () -> Unit, onEnemyKilled: (Chicken) -> Unit) {
        val collidedChicken = chickens.firstOrNull { CollisionUtils.isColliding(it.getRect(), player.getRect()) }
        if (collidedChicken != null) {
            if (!player.hasShield) {
                onPlayerHit()
            }
            onEnemyKilled(collidedChicken)
            chickens.remove(collidedChicken)
        }
    }
    
    open fun handleBossCollision(boss: BossChicken?, bullets: MutableList<Bullet>, onPlayerHit: () -> Unit, onBossDefeated: () -> Unit) {
        boss?.let { b ->
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
                    onPlayerHit()
                }
            }
            
            if (b.hp <= 0) {
                onBossDefeated()
            }
        }
    }

    /** Gọi khi quái chết để random thả mana tại (x,y) */
    fun spawnMana(x: Int, y: Int, manaBitmap: Bitmap, speed: Int = 10) {
        manaItems.add(Item(x, y, manaBitmap, ItemType.MANA, speed))
    }

    /** Cập nhật rơi & nhặt mana */
    open fun updateMana() {
        val it = manaItems.iterator()
        val playerRect: Rect = player.getRect()
        while (it.hasNext()) {
            val m = it.next()
            m.update()
            if (Rect.intersects(playerRect, m.getRect())) {
                manaCount++
                onManaCollected?.invoke(1)
                it.remove()
            } else if (scrH > 0 && m.y > scrH) {
                it.remove()
            }
        }
    }

    /** Vẽ các cục mana */
    open fun drawMana(canvas: Canvas) {
        manaItems.forEach { it.draw(canvas) }
    }

    /** Vẽ HUD số mana */
    open fun drawManaHUD(canvas: Canvas, paint: Paint, x: Float, y: Float) {
        paint.color = Color.CYAN
        paint.textSize = 40f
        canvas.drawText("Mana: $manaCount/$manaNeededForMissile", x, y, paint)
    }

    /** Dùng tên lửa, truyền vào danh sách quái, xóa sạch nếu đủ mana */
    open fun useMissile(enemies: MutableList<Chicken>): Boolean {
        if (manaCount >= manaNeededForMissile) {
            manaCount -= manaNeededForMissile
            enemies.clear()
            // Có thể thêm hiệu ứng nổ ở đây
            return true
        }
        return false
    }

    // --- Interface cho các Level kế thừa ---
    abstract fun update(bullets: MutableList<Bullet>)
    abstract fun draw(canvas: Canvas, bullets: List<Bullet>, backgroundY: Float = 0f)
    abstract fun isCompleted(): Boolean
    abstract fun reset()
    abstract fun getBackground(): Bitmap
    abstract fun getLives(): Int
    abstract fun isBossSpawned(): Boolean
    
    // Memory cleanup method
    open fun cleanup() {
        try {
            android.util.Log.d("BaseLevel", "Cleaning up level resources...")
            
            // Clear collections
            coins.clear()
            shields.clear()
            healthItems.clear()
            items.clear()
            eggs.clear()
            manaItems.clear()
            
            // Subclasses should override this to cleanup their specific resources
            android.util.Log.d("BaseLevel", "Base level cleanup completed")
        } catch (e: Exception) {
            android.util.Log.e("BaseLevel", "Error during level cleanup: ${e.message}")
            e.printStackTrace()
        }
    }
}