// BaseLevel.kt
package com.example.chickenshooter.levels

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import com.example.chickenshooter.*

abstract class BaseLevel(
    val context: Context,
    val player: Player,
    val bulletBitmap: Bitmap,
    val itemBitmaps: List<Bitmap>,
    // thêm bitmap xu
    coinBitmap: Bitmap
) {
    open var pickedGunMode: GunMode? = null
    open val coinBitmap: Bitmap = coinBitmap
    // --- Coins system ---
    protected val coins = mutableListOf<Coin>()
    var onCoinCollected: ((amount: Int) -> Unit)? = null
    private var scrW = 0
    private var scrH = 0
    fun setScreenSize(w: Int, h: Int) { scrW = w; scrH = h }

    /** Gọi khi địch chết để thả xu tại (x,y) */
    open fun spawnCoin(enemyX: Int, enemyY: Int, enemyW: Int, enemyH: Int) {
        val coinX = enemyX + enemyW / 2 - coinBitmap.width / 2
        val coinY = enemyY + enemyH / 2 - coinBitmap.height / 2
        coins.add(Coin(coinX, coinY, coinBitmap))
    }


    /** Cập nhật rơi và nhặt xu (gọi mỗi frame trong update của LevelX) */
    open fun updateCoins() {
        val it = coins.iterator()
        val playerRect: Rect = player.getRect()
        while (it.hasNext()) {
            val c = it.next()
            c.update()
            if (!c.isCollected && Rect.intersects(playerRect, c.getRect())) {
                c.isCollected = true
                onCoinCollected?.invoke(1) // mỗi coin = 1 xu
                it.remove()
            } else if (scrH > 0 && c.y > scrH) {
                it.remove()
            }
        }
    }
    // Hàm cộng xu
    fun saveCoinsToSystem() {
        val coinsEarned = coins.size.toLong() // hoặc số coin bạn muốn cộng
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
    /** Vẽ xu (gọi trong draw của LevelX) */
    open fun drawCoins(canvas: Canvas) {
        coins.forEach { it.draw(canvas) }
    }

    // --- Interface cũ ---
    abstract fun update(bullets: MutableList<Bullet>)
    abstract fun draw(canvas: Canvas, bullets: List<Bullet>)
    abstract fun isCompleted(): Boolean
    abstract fun reset()
    abstract fun getBackground(): Bitmap
    abstract fun getLives(): Int
}
