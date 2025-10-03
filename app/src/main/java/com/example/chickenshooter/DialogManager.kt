package com.example.chickenshooter

import android.graphics.*
import android.view.MotionEvent
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

// Data classes cho dialogue system
enum class Speaker { JACK, BOSS, SOL, KICM, THIEN_AN, FIREFLIES }

data class DialogueLine(val speaker: Speaker, val text: String)

data class DialogueScene(val lines: List<DialogueLine>)

/**
 * DialogManager - Quản lý hệ thống hội thoại visual novel style
 * Hiển thị dialogue với avatar tròn và typewriter effect
 * REFACTORED: Non-blocking input handling and asset preloading
 */
class DialogManager(
    private val screenWidth: Int,
    private val screenHeight: Int,
    private val avatarMap: Map<Speaker, Bitmap>
) {
    private val queuedScenes = mutableListOf<DialogueScene>()
    private var currentScene: DialogueScene? = null
    private var currentLineIndex = 0
    private var currentCharIndex = 0
    private val isLineComplete = AtomicBoolean(false)
    private val isActive = AtomicBoolean(false)
    
    private var typewriterCounter = 0
    private val typewriterSpeed = 2 // Tốc độ gõ chữ (frame giữa mỗi ký tự)
    
    // Long press detection
    private var longPressStartTime = 0L
    private val longPressDuration = 600L
    private var isLongPressing = false
    
    // Preloaded resources - thread-safe access
    private val preloadedResources = AtomicReference<DialogueSceneResources?>(null)
    
    // Coroutine scope for typewriter effect
    private val typewriterScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    // Callback for when all dialogues are complete
    private var onAllDoneCallback: (() -> Unit)? = null
    
    // Paint objects
    private val overlayPaint = Paint().apply {
        color = Color.argb(180, 0, 0, 0) // Nền tối mờ
    }
    
    private val dialogBoxPaint = Paint().apply {
        color = Color.argb(240, 25, 25, 50) // Hộp thoại tối xanh
        isAntiAlias = true
    }
    
    private val speakerNamePaint = Paint().apply {
        color = Color.YELLOW
        textSize = 36f
        isAntiAlias = true
        typeface = Typeface.DEFAULT_BOLD
    }
    
    private val dialogTextPaint = Paint().apply {
        color = Color.WHITE
        textSize = 32f
        isAntiAlias = true
    }
    
    private val hintPaint = Paint().apply {
        color = Color.argb(150, 255, 255, 255)
        textSize = 24f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }
    
    private val avatarBorderPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 4f
        isAntiAlias = true
    }
    
    // Kích thước và vị trí dialogue box
    private val dialogBoxHeight = screenHeight * 0.3f
    private val dialogBoxTop = screenHeight - dialogBoxHeight - 50f
    private val dialogBoxMargin = 40f
    private val avatarSize = 100f
    private val avatarCenterX = dialogBoxMargin + avatarSize / 2 + 30f
    private val avatarCenterY = dialogBoxTop + dialogBoxHeight / 2
    
    fun queueScene(scene: DialogueScene) {
        try {
            queuedScenes.add(scene)
            android.util.Log.d("DialogManager", "Scene queued with ${scene.lines.size} lines")
        } catch (e: Exception) {
            android.util.Log.e("DialogManager", "Error queueing scene: ${e.message}")
        }
    }
    
    fun queueScene(scene: DialogueScene, onComplete: () -> Unit) {
        try {
            queuedScenes.add(scene)
            onAllDoneCallback = onComplete
            android.util.Log.d("DialogManager", "Scene queued with callback and ${scene.lines.size} lines")
        } catch (e: Exception) {
            android.util.Log.e("DialogManager", "Error queueing scene with callback: ${e.message}")
        }
    }
    
    fun startIfAny() {
        try {
            if (!isActive.get() && queuedScenes.isNotEmpty()) {
                currentScene = queuedScenes.removeAt(0)
                currentLineIndex = 0
                currentCharIndex = 0
                isLineComplete.set(false)
                isActive.set(true)
                typewriterCounter = 0
                android.util.Log.d("DialogManager", "Started dialogue scene with ${currentScene?.lines?.size ?: 0} lines")
            }
        } catch (e: Exception) {
            android.util.Log.e("DialogManager", "Error starting dialogue: ${e.message}")
        }
    }
    
    /**
     * Start scene with preloaded resources - NON-BLOCKING
     */
    fun startScene(scene: DialogueScene, resources: DialogueSceneResources) {
        try {
            android.util.Log.d("DialogManager", "Starting scene with preloaded resources")
            preloadedResources.set(resources)
            currentScene = scene
            currentLineIndex = 0
            currentCharIndex = 0
            isLineComplete.set(false)
            isActive.set(true)
            typewriterCounter = 0
            android.util.Log.d("DialogManager", "Scene started with ${scene.lines.size} lines")
        } catch (e: Exception) {
            android.util.Log.e("DialogManager", "Error starting scene: ${e.message}")
        }
    }
    
    fun update() {
        if (!isActive.get() || currentScene == null) return
        
        // Typewriter effect - NON-BLOCKING
        if (!isLineComplete.get()) {
            typewriterCounter++
            if (typewriterCounter >= typewriterSpeed) {
                val currentLine = getCurrentLine()
                if (currentLine != null && currentCharIndex < currentLine.text.length) {
                    currentCharIndex++
                } else {
                    isLineComplete.set(true)
                }
                typewriterCounter = 0
            }
        }
    }
    
    /**
     * Handle input events from GameThread - NON-BLOCKING
     */
    fun handleInput(event: InputEvent) {
        if (!isActive.get()) return
        
        when (event) {
            is InputEvent.NextDialogue -> {
                android.util.Log.d("DialogManager", "NextDialogue input received")
                nextOrCompleteLine()
            }
            is InputEvent.Touch -> {
                android.util.Log.d("DialogManager", "Touch input received: ${event.action}")
                handleTouchInput(event)
            }
            is InputEvent.LongPress -> {
                android.util.Log.d("DialogManager", "LongPress input received")
                skipCurrentScene()
            }
        }
    }
    
    private fun handleTouchInput(event: InputEvent.Touch) {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                longPressStartTime = event.timestamp
                isLongPressing = false
            }
            MotionEvent.ACTION_UP -> {
                val pressDuration = event.timestamp - longPressStartTime
                if (pressDuration >= longPressDuration) {
                    skipCurrentScene()
                } else {
                    nextOrCompleteLine()
                }
            }
            MotionEvent.ACTION_MOVE -> {
                val pressDuration = event.timestamp - longPressStartTime
                if (pressDuration >= longPressDuration && !isLongPressing) {
                    isLongPressing = true
                    skipCurrentScene()
                }
            }
        }
    }
    
    /**
     * Complete current line or advance to next - NON-BLOCKING
     */
    fun nextOrCompleteLine() {
        if (!isLineComplete.get()) {
            // Complete current line immediately
            val currentLine = getCurrentLine()
            if (currentLine != null) {
                currentCharIndex = currentLine.text.length
                isLineComplete.set(true)
                android.util.Log.d("DialogManager", "Line completed immediately")
            }
        } else {
            // Advance to next line
            nextLine()
        }
    }
    
    fun draw(canvas: Canvas) {
        if (!isActive.get() || currentScene == null) return
        
        val currentLine = getCurrentLine() ?: return
        
        // Vẽ overlay
        canvas.drawRect(0f, 0f, screenWidth.toFloat(), screenHeight.toFloat(), overlayPaint)
        
        // Vẽ dialogue box với góc bo tròn
        val boxLeft = dialogBoxMargin
        val boxRight = screenWidth - dialogBoxMargin
        val boxRect = RectF(boxLeft, dialogBoxTop, boxRight, dialogBoxTop + dialogBoxHeight)
        val cornerRadius = 25f
        canvas.drawRoundRect(boxRect, cornerRadius, cornerRadius, dialogBoxPaint)
        
        // Vẽ viền dialogue box
        val borderPaint = Paint().apply {
            color = Color.argb(100, 255, 255, 255)
            style = Paint.Style.STROKE
            strokeWidth = 2f
            isAntiAlias = true
        }
        canvas.drawRoundRect(boxRect, cornerRadius, cornerRadius, borderPaint)
        
        // Vẽ avatar tròn
        val avatar = avatarMap[currentLine.speaker]
        if (avatar != null) {
            drawCircularAvatar(canvas, avatar, avatarCenterX, avatarCenterY, avatarSize / 2)
        }
        
        // Vẽ tên speaker
        val speakerName = getSpeakerDisplayName(currentLine.speaker)
        val nameX = avatarCenterX + avatarSize / 2 + 30f
        val nameY = dialogBoxTop + 50f
        canvas.drawText(speakerName, nameX, nameY, speakerNamePaint)
        
        // Vẽ text với word wrapping
        val displayText = currentLine.text.substring(0, currentCharIndex)
        val textStartX = nameX
        val textStartY = nameY + 50f
        val maxTextWidth = boxRight - textStartX - 30f
        val lineHeight = dialogTextPaint.textSize * 1.4f
        
        val wrappedLines = wrapText(displayText, dialogTextPaint, maxTextWidth)
        var textY = textStartY
        
        for (line in wrappedLines) {
            canvas.drawText(line, textStartX, textY, dialogTextPaint)
            textY += lineHeight
        }
        
        // Vẽ cursor nhấp nháy
        if (!isLineComplete.get() && currentCharIndex < currentLine.text.length) {
            val cursorCounter = (System.currentTimeMillis() / 400) % 2
            if (cursorCounter == 0L) {
                val lastLine = if (wrappedLines.isNotEmpty()) wrappedLines.last() else ""
                val cursorX = textStartX + dialogTextPaint.measureText(lastLine)
                val cursorY = textY - lineHeight + dialogTextPaint.textSize
                canvas.drawText("_", cursorX, cursorY, dialogTextPaint)
            }
        }
        
        // Vẽ hướng dẫn
        val hintY = screenHeight - 30f
        if (isLineComplete.get()) {
            if (hasMoreLines()) {
                canvas.drawText("Chạm để tiếp tục...", screenWidth / 2f, hintY, hintPaint)
            } else {
                canvas.drawText("Chạm để kết thúc hội thoại", screenWidth / 2f, hintY, hintPaint)
            }
        } else {
            canvas.drawText("Chạm để hiển thị ngay | Giữ lâu để bỏ qua scene", screenWidth / 2f, hintY, hintPaint)
        }
        
        // Hiển thị tiến độ hội thoại
        val progress = "${currentLineIndex + 1}/${currentScene?.lines?.size ?: 0}"
        val progressPaint = Paint().apply {
            color = Color.argb(120, 255, 255, 255)
            textSize = 20f
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText(progress, boxRight - 20f, dialogBoxTop - 10f, progressPaint)
    }
    
    private fun drawCircularAvatar(canvas: Canvas, bitmap: Bitmap, centerX: Float, centerY: Float, radius: Float) {
        val paint = Paint().apply {
            isAntiAlias = true
        }
        
        // Tạo circular clip
        val savedLayer = canvas.saveLayer(
            centerX - radius, centerY - radius, 
            centerX + radius, centerY + radius, 
            null
        )
        
        // Vẽ circle mask
        canvas.drawCircle(centerX, centerY, radius, paint)
        
        // Áp dụng SRC_IN để chỉ giữ phần trong circle
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        
        // Scale và vẽ bitmap
        val scaledBitmap = Bitmap.createScaledBitmap(
            bitmap, 
            (radius * 2).toInt(), 
            (radius * 2).toInt(), 
            true
        )
        canvas.drawBitmap(scaledBitmap, centerX - radius, centerY - radius, paint)
        
        // Restore layer
        canvas.restoreToCount(savedLayer)
        
        // Vẽ viền trắng
        canvas.drawCircle(centerX, centerY, radius, avatarBorderPaint)
    }
    
    /**
     * DEPRECATED: Use handleInput() instead for non-blocking operation
     * This method is kept for backward compatibility but should not be used
     */
    @Deprecated("Use handleInput() for non-blocking operation")
    fun onTouch(event: MotionEvent, onAllDone: () -> Unit): Boolean {
        if (!isActive.get()) {
            android.util.Log.d("DialogManager", "onTouch called but not active")
            return false
        }

        android.util.Log.d("DialogManager", "onTouch: action=${event.action}, isActive=${isActive.get()}, currentScene=${currentScene != null}")

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                longPressStartTime = System.currentTimeMillis()
                isLongPressing = false
                android.util.Log.d("DialogManager", "Touch DOWN detected")
                return true
            }
            
            MotionEvent.ACTION_UP -> {
                val pressDuration = System.currentTimeMillis() - longPressStartTime
                android.util.Log.d("DialogManager", "Touch UP: duration=${pressDuration}ms, isLineComplete=${isLineComplete.get()}")
                
                if (pressDuration >= longPressDuration) {
                    // Long press - skip toàn bộ scene hiện tại
                    android.util.Log.d("DialogManager", "Long press detected - skipping scene")
                    skipCurrentScene(onAllDone)
                } else {
                    // Short tap
                    if (!isLineComplete.get()) {
                        // Hiển thị ngay toàn bộ dòng
                        val currentLine = getCurrentLine()
                        if (currentLine != null) {
                            android.util.Log.d("DialogManager", "Completing current line: ${currentLine.text}")
                            currentCharIndex = currentLine.text.length
                            isLineComplete.set(true)
                        }
                    } else {
                        // Chuyển sang dòng tiếp theo
                        android.util.Log.d("DialogManager", "Moving to next line")
                        nextLine(onAllDone)
                    }
                }
                return true
            }
            
            MotionEvent.ACTION_MOVE -> {
                val pressDuration = System.currentTimeMillis() - longPressStartTime
                if (pressDuration >= longPressDuration && !isLongPressing) {
                    isLongPressing = true
                    skipCurrentScene(onAllDone)
                }
                return true
            }
        }
        
        return false
    }
    
    private fun nextLine(onAllDone: () -> Unit) {
        val scene = currentScene ?: return
        
        android.util.Log.d("DialogManager", "nextLine: currentLineIndex=$currentLineIndex, totalLines=${scene.lines.size}")
        
        if (currentLineIndex < scene.lines.size - 1) {
            currentLineIndex++
            currentCharIndex = 0
            isLineComplete.set(false)
            typewriterCounter = 0
            android.util.Log.d("DialogManager", "Advanced to line ${currentLineIndex + 1}/${scene.lines.size}")
        } else {
            // Kết thúc scene hiện tại
            android.util.Log.d("DialogManager", "Scene completed, finishing...")
            finishCurrentScene(onAllDone)
        }
    }
    
    private fun nextLine() {
        val scene = currentScene ?: return
        
        android.util.Log.d("DialogManager", "nextLine: currentLineIndex=$currentLineIndex, totalLines=${scene.lines.size}")
        
        if (currentLineIndex < scene.lines.size - 1) {
            currentLineIndex++
            currentCharIndex = 0
            isLineComplete.set(false)
            typewriterCounter = 0
            android.util.Log.d("DialogManager", "Advanced to line ${currentLineIndex + 1}/${scene.lines.size}")
        } else {
            // Kết thúc scene hiện tại
            android.util.Log.d("DialogManager", "Scene completed, finishing...")
            finishCurrentScene()
        }
    }
    
    private fun skipCurrentScene(onAllDone: () -> Unit) {
        finishCurrentScene(onAllDone)
    }
    
    private fun skipCurrentScene() {
        finishCurrentScene()
    }
    
    private fun finishCurrentScene(onAllDone: () -> Unit) {
        android.util.Log.d("DialogManager", "finishCurrentScene: remaining queued scenes = ${queuedScenes.size}")
        currentScene = null
        isActive.set(false)
        
        // Kiểm tra xem còn scene nào trong queue không
        if (queuedScenes.isNotEmpty()) {
            android.util.Log.d("DialogManager", "Starting next queued scene...")
            startIfAny()
        } else {
            // Tất cả scene đã hoàn thành
            android.util.Log.d("DialogManager", "All scenes completed, calling onAllDone")
            onAllDone()
        }
    }
    
    private fun finishCurrentScene() {
        android.util.Log.d("DialogManager", "finishCurrentScene: remaining queued scenes = ${queuedScenes.size}")
        currentScene = null
        isActive.set(false)
        
        // Kiểm tra xem còn scene nào trong queue không
        if (queuedScenes.isNotEmpty()) {
            android.util.Log.d("DialogManager", "Starting next queued scene...")
            startIfAny()
        } else {
            // Tất cả scene đã hoàn thành
            android.util.Log.d("DialogManager", "All scenes completed")
            onAllDoneCallback?.invoke()
            onAllDoneCallback = null
        }
    }
    
    private fun getCurrentLine(): DialogueLine? {
        val scene = currentScene ?: return null
        return if (currentLineIndex < scene.lines.size) {
            scene.lines[currentLineIndex]
        } else null
    }
    
    private fun hasMoreLines(): Boolean {
        val scene = currentScene ?: return false
        return currentLineIndex < scene.lines.size - 1
    }
    
    private fun getSpeakerDisplayName(speaker: Speaker): String {
        return when (speaker) {
            Speaker.JACK -> "Jack"
            Speaker.BOSS -> "Boss Gà"
            Speaker.SOL -> "Sol"
            Speaker.KICM -> "K-ICM"
            Speaker.THIEN_AN -> "Thiên An"
            Speaker.FIREFLIES -> "Đom Đóm"
        }
    }
    
    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = ""
        
        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            val textWidth = paint.measureText(testLine)
            
            if (textWidth <= maxWidth) {
                currentLine = testLine
            } else {
                if (currentLine.isNotEmpty()) {
                    lines.add(currentLine)
                    currentLine = word
                } else {
                    // Từ quá dài, cắt theo ký tự
                    val chars = word.toCharArray()
                    var charLine = ""
                    for (char in chars) {
                        val testCharLine = charLine + char
                        if (paint.measureText(testCharLine) <= maxWidth) {
                            charLine = testCharLine
                        } else {
                            if (charLine.isNotEmpty()) {
                                lines.add(charLine)
                            }
                            charLine = char.toString()
                        }
                    }
                    currentLine = charLine
                }
            }
        }
        
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine)
        }
        
        return lines
    }
    
    /**
     * Trả về vị trí trung tâm của avatar dock để RescueAvatar bay tới
     */
    fun avatarDockCenter(): PointF {
        try {
            val center = PointF(avatarCenterX, avatarCenterY)
            android.util.Log.d("DialogManager", "Avatar dock center: (${center.x}, ${center.y})")
            return center
        } catch (e: Exception) {
            android.util.Log.e("DialogManager", "Error getting avatar dock center: ${e.message}")
            // Fallback to screen center
            return PointF(screenWidth / 2f, screenHeight / 2f)
        }
    }
    
    fun isActiveOrHasQueue(): Boolean = isActive.get() || queuedScenes.isNotEmpty()
    
    /**
     * Cleanup resources and cancel coroutines
     */
    fun cleanup() {
        try {
            android.util.Log.d("DialogManager", "Cleaning up DialogManager resources...")
            typewriterScope.cancel()
            preloadedResources.set(null)
            queuedScenes.clear()
            currentScene = null
            isActive.set(false)
            android.util.Log.d("DialogManager", "DialogManager cleanup completed")
        } catch (e: Exception) {
            android.util.Log.e("DialogManager", "Error during DialogManager cleanup: ${e.message}")
            e.printStackTrace()
        }
    }
}
