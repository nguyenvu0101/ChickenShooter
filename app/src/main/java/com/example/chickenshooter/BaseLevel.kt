package com.example.chickenshooter.levels

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.Paint
import android.graphics.Color
import com.example.chickenshooter.*

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
    // --- Coins system ---
    protected val coins = mutableListOf<Coin>()
    var onCoinCollected: ((amount: Int) -> Unit)? = null
    private var scrW = 0
    private var scrH = 0

    fun setScreenSize(w: Int, h: Int) {
        scrW = w
        scrH = h
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

    /** Lưu xu về hệ thống (local hoặc online) */
    fun saveCoinsToSystem() {
        val coinsEarned = coins.size.toLong()
        val isOffline = UserRepoRTDB().isOffline(context)
        if (isOffline) {
            UserRepoRTDB().addOfflineCoins(context, coinsEarned)
        } else {
            val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            if (uid != null) {
                UserRepoRTDB().addCoins(uid, coinsEarned)
            }
        }
    }

    /** Vẽ xu */
    open fun drawCoins(canvas: Canvas) {
        coins.forEach { it.draw(canvas) }
    }

    // ----------- Mana System for Missile ------------
    var manaCount = 0
    val manaNeededForMissile = 4
    protected val manaItems = mutableListOf<Item>()
    var onManaCollected: ((amount: Int) -> Unit)? = null

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
    abstract fun draw(canvas: Canvas, bullets: List<Bullet>)
    abstract fun isCompleted(): Boolean
    abstract fun reset()
    abstract fun getBackground(): Bitmap
    abstract fun getLives(): Int
}