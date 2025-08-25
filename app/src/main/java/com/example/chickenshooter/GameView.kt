package com.example.chickenshooter

import android.content.Context
import android.graphics.Canvas
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView

class GameView(context: Context) : SurfaceView(context), SurfaceHolder.Callback {
    private val thread: GameThread
    private var currentLevelIndex = 1
    private val maxLevel = 4
    private lateinit var player: Player
    private lateinit var currentLevel: BaseLevel

    init {
        holder.addCallback(this)
        thread = GameThread(holder, this)
        isFocusable = true
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        // Khởi tạo player ở đây (có thể dùng lại qua các màn)
        player = Player(width / 2 - 64, height - 164, null)
        startLevel(1)
        thread.running = true
        thread.start()
    }

    fun startLevel(level: Int) {
        currentLevelIndex = level
        currentLevel = when (level) {
            1 -> Level1(context, this, player)
            2 -> Level2(context, this, player)
            3 -> Level3(context, this, player)
            4 -> Level4(context, this, player)
            else -> Level1(context, this, player)
        }
        currentLevel.initLevel()
    }

    fun nextLevel() {
        if (currentLevelIndex < maxLevel) {
            startLevel(currentLevelIndex + 1)
        } else {
            currentLevel.showVictory()
        }
    }

    fun update() {
        currentLevel.update()
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        currentLevel.draw(canvas)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        currentLevel.onTouch(event)
        return true
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        var retry = true
        thread.running = false
        while (retry) {
            try {
                thread.join()
                retry = false
            } catch (e: InterruptedException) {}
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

    inner class GameThread(private val surfaceHolder: SurfaceHolder, private val gameView: GameView) : Thread() {
        var running = false
        override fun run() {
            while (running) {
                val canvas: Canvas? = surfaceHolder.lockCanvas()
                if (canvas != null) {
                    synchronized(surfaceHolder) {
                        gameView.update()
                        gameView.draw(canvas)
                    }
                    surfaceHolder.unlockCanvasAndPost(canvas)
                }
                sleep(16)
            }
        }
    }
}