package com.example.chickenshooter

import android.graphics.*
import android.view.MotionEvent

/**
 * IntroManager - Quản lý màn hình intro với typewriter effect
 * Hiển thị câu chuyện mở đầu với hiệu ứng gõ chữ
 */
class IntroManager(
    private val screenWidth: Int,
    private val screenHeight: Int,
    private val onIntroFinished: () -> Unit
) {
    // Danh sách câu chuyện intro
    private val storyLines = listOf(
        "Giữa biển sao vô tận, quân đoàn Gà Thiên Hà đã lan tràn đến Trái đất.",
        "Chúng đã bắt giữ những người quan trọng với Jack: bé Sol, K-ICM và Thiên An.",
        "Jack phải khởi động phi thuyền J97.",
        "Xuyên qua các vì sao, chiến đấu với quân đoàn gà là nhiệm vụ của anh."
    )
    
    private var currentLineIndex = 0
    private var currentCharIndex = 0
    private var isLineComplete = false
    private var isIntroComplete = false
    private var typewriterCounter = 0
    private val typewriterSpeed = 3 // Số frame giữa mỗi ký tự (3 frame = ~50ms ở 60fps)
    
    // Long press detection
    private var longPressStartTime = 0L
    private val longPressDuration = 600L // 600ms
    private var isLongPressing = false
    
    // Paint objects cho vẽ
    private val overlayPaint = Paint().apply {
        color = Color.argb(200, 0, 0, 0) // Nền tối mờ
    }
    
    private val boxPaint = Paint().apply {
        color = Color.argb(240, 20, 20, 40) // Hộp thoại tối
        isAntiAlias = true
    }
    
    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 40f
        isAntiAlias = true
        typeface = Typeface.DEFAULT_BOLD
    }
    
    private val hintPaint = Paint().apply {
        color = Color.argb(180, 255, 255, 255)
        textSize = 28f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }
    
    fun update() {
        if (isIntroComplete) return
        
        // Typewriter effect
        if (!isLineComplete) {
            typewriterCounter++
            if (typewriterCounter >= typewriterSpeed) {
                val currentLine = storyLines[currentLineIndex]
                if (currentCharIndex < currentLine.length) {
                    currentCharIndex++
                } else {
                    isLineComplete = true
                }
                typewriterCounter = 0
            }
        }
    }
    
    fun draw(canvas: Canvas) {
        if (isIntroComplete) return
        
        // Vẽ overlay tối
        canvas.drawRect(0f, 0f, screenWidth.toFloat(), screenHeight.toFloat(), overlayPaint)
        
        // Tính toán kích thước và vị trí hộp story
        val boxWidth = screenWidth * 0.85f
        val boxHeight = screenHeight * 0.4f
        val boxLeft = (screenWidth - boxWidth) / 2
        val boxTop = (screenHeight - boxHeight) / 2
        val cornerRadius = 20f
        
        // Vẽ hộp story với góc bo tròn
        val boxRect = RectF(boxLeft, boxTop, boxLeft + boxWidth, boxTop + boxHeight)
        canvas.drawRoundRect(boxRect, cornerRadius, cornerRadius, boxPaint)
        
        // Vẽ viền hộp
        val borderPaint = Paint().apply {
            color = Color.argb(150, 255, 255, 255)
            style = Paint.Style.STROKE
            strokeWidth = 3f
            isAntiAlias = true
        }
        canvas.drawRoundRect(boxRect, cornerRadius, cornerRadius, borderPaint)
        
        // Vẽ text với word wrapping
        val currentLine = storyLines[currentLineIndex]
        val displayText = currentLine.substring(0, currentCharIndex)
        
        val textMargin = 40f
        val lineHeight = textPaint.textSize * 1.5f
        val maxTextWidth = boxWidth - (textMargin * 2)
        
        // Word wrap và vẽ text
        val wrappedLines = wrapText(displayText, textPaint, maxTextWidth)
        var textY = boxTop + textMargin + textPaint.textSize
        
        for (line in wrappedLines) {
            canvas.drawText(line, boxLeft + textMargin, textY, textPaint)
            textY += lineHeight
        }
        
        // Vẽ cursor nhấp nháy nếu dòng chưa hoàn thành
        if (!isLineComplete && currentCharIndex < currentLine.length) {
            val cursorCounter = (System.currentTimeMillis() / 500) % 2
            if (cursorCounter == 0L) {
                val lastLine = if (wrappedLines.isNotEmpty()) wrappedLines.last() else ""
                val cursorX = boxLeft + textMargin + textPaint.measureText(lastLine)
                val cursorY = textY - lineHeight + textPaint.textSize
                canvas.drawText("|", cursorX, cursorY, textPaint)
            }
        }
        
        // Vẽ hướng dẫn tương tác
        val hintY = boxTop + boxHeight + 50f
        if (isLineComplete) {
            if (currentLineIndex < storyLines.size - 1) {
                canvas.drawText("Chạm để tiếp tục...", screenWidth / 2f, hintY, hintPaint)
            } else {
                canvas.drawText("Chạm để bắt đầu game!", screenWidth / 2f, hintY, hintPaint)
            }
        } else {
            canvas.drawText("Chạm để hiển thị ngay | Giữ lâu để bỏ qua", screenWidth / 2f, hintY, hintPaint)
        }
        
        // Hiển thị tiến độ
        val progressText = "${currentLineIndex + 1}/${storyLines.size}"
        val progressPaint = Paint().apply {
            color = Color.argb(120, 255, 255, 255)
            textSize = 24f
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText(progressText, boxLeft + boxWidth - 20f, boxTop - 20f, progressPaint)
    }
    
    fun onTouch(event: MotionEvent): Boolean {
        if (isIntroComplete) return false
        
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                longPressStartTime = System.currentTimeMillis()
                isLongPressing = false
                return true
            }
            
            MotionEvent.ACTION_UP -> {
                val pressDuration = System.currentTimeMillis() - longPressStartTime
                
                if (pressDuration >= longPressDuration) {
                    // Long press - skip toàn bộ intro
                    skipIntro()
                } else {
                    // Short tap
                    if (!isLineComplete) {
                        // Nếu dòng chưa hoàn thành, hiển thị ngay toàn bộ dòng
                        currentCharIndex = storyLines[currentLineIndex].length
                        isLineComplete = true
                    } else {
                        // Nếu dòng đã hoàn thành, chuyển sang dòng tiếp theo
                        nextLine()
                    }
                }
                return true
            }
            
            MotionEvent.ACTION_MOVE -> {
                // Kiểm tra long press
                val pressDuration = System.currentTimeMillis() - longPressStartTime
                if (pressDuration >= longPressDuration && !isLongPressing) {
                    isLongPressing = true
                    skipIntro()
                }
                return true
            }
        }
        
        return false
    }
    
    private fun nextLine() {
        if (currentLineIndex < storyLines.size - 1) {
            currentLineIndex++
            currentCharIndex = 0
            isLineComplete = false
            typewriterCounter = 0
        } else {
            // Kết thúc intro
            finishIntro()
        }
    }
    
    private fun skipIntro() {
        finishIntro()
    }
    
    private fun finishIntro() {
        android.util.Log.d("IntroManager", "Intro finishing...")
        isIntroComplete = true
        android.util.Log.d("IntroManager", "Calling onIntroFinished callback")
        onIntroFinished()
        android.util.Log.d("IntroManager", "onIntroFinished callback completed")
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
    
    fun isFinished(): Boolean = isIntroComplete
}
