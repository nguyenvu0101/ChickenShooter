package com.example.chickenshooter

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color
import android.view.MotionEvent

class Level1(context: Context, gameView: GameView, player: Player) : BaseLevel(context, gameView, player) {
    private val background = BitmapFactory.decodeResource(context.resources, R.drawable.background)
    private val bossBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.boss_chicken)
    private val playerBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.player)
    private var boss: BossChicken? = null
    private var waitingForBoss = false
    private val chickens = mutableListOf<Chicken>()
    private val bullets = mutableListOf<Bullet>()
    private val items = mutableListOf<Item>()
    private var levelTimer = 0
    private val levelDuration = 60 * 60
    private var lives = 3
    private var isPlayerHit = false
    private var hitCooldown = 0
    private val hitCooldownDuration = 40
    private var spawnCooldown = 0
    private val chickenBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.chicken1)
    private val bossHp = 40
    private val bossSpeed = 5

    override fun initLevel() {
        player.bitmap = playerBitmap
        chickens.clear()
        bullets.clear()
        items.clear()
        boss = null
        waitingForBoss = false
        levelTimer = 0
        lives = 3
        isPlayerHit = false
    }

    override fun update() {
        // Spawn chicken logic, update all chickens, bullets, items, etc.
        if (levelTimer < levelDuration && !waitingForBoss) {
            spawnCooldown++
            if (spawnCooldown > 40) {
                val randX = (0..(gameView.width - chickenBitmap.width)).random()
                chickens.add(Chicken(randX, 0, chickenBitmap, 6, 0, 3))
                spawnCooldown = 0
            }
        }

        chickens.forEach { it.update() }
        chickens.removeAll { it.y > gameView.height }

        // Update boss
        if (levelTimer >= levelDuration) {
            if (!waitingForBoss && boss == null && chickens.isEmpty()) {
                boss = BossChicken(
                    x = gameView.width / 2 - bossBitmap.width / 2,
                    y = 0,
                    bitmap = bossBitmap,
                    hp = bossHp,
                    speed = bossSpeed
                )
                waitingForBoss = true
            }
        }
        boss?.update()

        // ... Cập nhật đạn, va chạm, item, mạng, v.v. (như phần cũ)

        // Nếu boss chết, sang màn mới
        if (boss?.hp ?: 1 <= 0) {
            gameView.nextLevel()
        }
        levelTimer++
    }

    override fun draw(canvas: Canvas) {
        canvas.drawBitmap(background, 0f, 0f, null)
        player.draw(canvas)
        chickens.forEach { it.draw(canvas) }
        bullets.forEach { it.draw(canvas) }
        items.forEach { it.draw(canvas) }
        boss?.draw(canvas)
        // ...Vẽ mạng, hiệu ứng, thông tin, v.v.
    }

    override fun onTouch(event: MotionEvent) {
        if (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_MOVE) {
            val newX = event.x.toInt() - player.bitmap!!.width / 2
            player.x = newX.coerceIn(0, gameView.width - player.bitmap!!.width)
        }
    }

    override fun showVictory() {
        // Hiện hiệu ứng thắng màn cuối nếu muốn
    }
}