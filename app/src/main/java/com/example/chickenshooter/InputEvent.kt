package com.example.chickenshooter

import android.view.MotionEvent

/**
 * Input event system for non-blocking touch handling
 * Events are queued on UI thread and processed on GameThread
 */
sealed class InputEvent {
    object NextDialogue : InputEvent()
    data class Touch(val x: Float, val y: Float, val action: Int, val timestamp: Long) : InputEvent()
    data class LongPress(val x: Float, val y: Float, val timestamp: Long) : InputEvent()
}

/**
 * Input queue for thread-safe communication between UI and GameThread
 */
class InputQueue {
    private val queue = java.util.concurrent.ConcurrentLinkedQueue<InputEvent>()
    
    fun enqueue(event: InputEvent) {
        queue.offer(event)
    }
    
    fun poll(): InputEvent? = queue.poll()
    
    fun isEmpty(): Boolean = queue.isEmpty()
    
    fun clear() = queue.clear()
}
