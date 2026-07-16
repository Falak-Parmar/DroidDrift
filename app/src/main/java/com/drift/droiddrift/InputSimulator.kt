package com.drift.droiddrift

import android.hardware.input.InputManager
import android.os.SystemClock
import android.view.InputEvent
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.InputDevice
import android.util.Log

class InputSimulator {
    private val TAG = "InputSimulator"
    private var injectMethod: java.lang.reflect.Method? = null
    private var inputManager: Any? = null
    
    // Virtual absolute cursor position tracking for relative deltas
    private var currentX = 500f
    private var currentY = 500f
    private var lastEntryTime: Long = 0
    
    // Screen dimensions (will be updated dynamically by MainActivity)
    var screenWidth = 1080
    var screenHeight = 2400

    var onExitLeft: (() -> Unit)? = null

    init {
        // Try standard App/Activity context lookup first
        try {
            val getInstanceMethod = InputManager::class.java.getMethod("getInstance")
            inputManager = getInstanceMethod.invoke(null)
            if (inputManager != null) {
                injectMethod = InputManager::class.java.getMethod(
                    "injectInputEvent", 
                    InputEvent::class.java, 
                    Int::class.javaPrimitiveType
                )
                Log.d(TAG, "InputManager reflection successful (App context).")
            }
        } catch (e: Exception) {
            Log.d(TAG, "Standard InputManager.getInstance() failed: ${e.message}. Trying service manager...")
        }
        
        // Fallback for ADB shell app_process (no Context/ActivityThread initialized)
        if (inputManager == null) {
            try {
                val serviceManagerClass = Class.forName("android.os.ServiceManager")
                val getServiceMethod = serviceManagerClass.getMethod("getService", String::class.java)
                val binder = getServiceMethod.invoke(null, "input") as android.os.IBinder
                
                val iInputManagerStubClass = Class.forName("android.hardware.input.IInputManager\$Stub")
                val asInterfaceMethod = iInputManagerStubClass.getMethod("asInterface", android.os.IBinder::class.java)
                inputManager = asInterfaceMethod.invoke(null, binder)
                
                if (inputManager != null) {
                    injectMethod = inputManager!!.javaClass.getMethod(
                        "injectInputEvent", 
                        InputEvent::class.java, 
                        Int::class.javaPrimitiveType
                    )
                    Log.d(TAG, "InputManager reflection successful (ServiceManager/Binder context).")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize reflection for InputManager binder: ${e.message}")
            }
        }
    }

    /// Creates a mouse event containing pointer coordinates, properties, and correct buttonState
    private fun obtainMouseEvent(action: Int, x: Float, y: Float, buttonState: Int = 0): MotionEvent {
        val time = SystemClock.uptimeMillis()
        val properties = MotionEvent.PointerProperties().apply {
            id = 0
            toolType = MotionEvent.TOOL_TYPE_MOUSE
        }
        val coords = MotionEvent.PointerCoords().apply {
            this.x = x
            this.y = y
            pressure = if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) 1.0f else 0.0f
            size = 1.0f
        }
        return MotionEvent.obtain(
            time, time, action,
            1, arrayOf(properties), arrayOf(coords),
            0, buttonState, 1.0f, 1.0f,
            0, 0, InputDevice.SOURCE_MOUSE, 0
        )
    }

    fun injectMouseMove(dx: Float, dy: Float) {
        val nextX = currentX + dx
        val nextY = (currentY + dy).coerceIn(0f, screenHeight.toFloat())
        
        // EXIT BOUNDARY TRIGGER:
        // If cursor moves past the right boundary (x > screenWidth) and we were at screenWidth, trigger exit back to Mac!
        if (nextX > screenWidth) {
            currentX = screenWidth.toFloat()
            val timeSinceEntry = SystemClock.uptimeMillis() - lastEntryTime
            if (timeSinceEntry > 500) { // 500ms temporal cooldown to prevent instant exit loops
                onExitLeft?.invoke()
            }
            return
        }
        
        currentX = nextX.coerceIn(0f, screenWidth.toFloat())
        currentY = nextY
        
        val event = obtainMouseEvent(MotionEvent.ACTION_HOVER_MOVE, currentX, currentY)
        injectEvent(event)
    }
    
    fun injectMouseButton(button: String, state: String) {
        val action = if (state == "down") MotionEvent.ACTION_DOWN else MotionEvent.ACTION_UP
        val buttonState = if (button == "left") MotionEvent.BUTTON_PRIMARY else MotionEvent.BUTTON_SECONDARY
        
        val event = obtainMouseEvent(action, currentX, currentY, buttonState)
        injectEvent(event)
    }

    fun injectKey(keycode: Int, state: String) {
        val action = if (state == "down") KeyEvent.ACTION_DOWN else KeyEvent.ACTION_UP
        val time = SystemClock.uptimeMillis()
        
        // Map common macOS keycodes to Android keycodes
        val androidKeyCode = mapMacToAndroidKey(keycode)
        
        val event = KeyEvent(time, time, action, androidKeyCode, 0)
        event.source = InputDevice.SOURCE_KEYBOARD
        injectEvent(event)
    }
    
    fun injectScroll(dx: Float, dy: Float) {
        val time = SystemClock.uptimeMillis()
        val properties = MotionEvent.PointerProperties().apply {
            id = 0
            toolType = MotionEvent.TOOL_TYPE_MOUSE
        }
        val coords = MotionEvent.PointerCoords().apply {
            this.x = currentX
            this.y = currentY
            setAxisValue(MotionEvent.AXIS_VSCROLL, dy)
            setAxisValue(MotionEvent.AXIS_HSCROLL, -dx)
        }
        
        val event = MotionEvent.obtain(
            time, time, MotionEvent.ACTION_SCROLL,
            1, arrayOf(properties), arrayOf(coords),
            0, 0, 1.0f, 1.0f,
            0, 0, InputDevice.SOURCE_MOUSE, 0
        )
        injectEvent(event)
    }
    
    private fun injectEvent(event: InputEvent) {
        try {
            injectMethod?.invoke(inputManager, event, 0)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to inject event: ${e.message}")
        }
    }
    
    fun resetCursor(yRatio: Float) {
        lastEntryTime = SystemClock.uptimeMillis()
        currentX = screenWidth.toFloat() - 30f // Start slightly inside the screen to prevent instant exits
        currentY = yRatio * screenHeight
        Log.d(TAG, "Cursor reset to right edge with buffer: ($currentX, $currentY)")
        
        val event = obtainMouseEvent(MotionEvent.ACTION_HOVER_MOVE, currentX, currentY)
        injectEvent(event)
    }

    private fun mapMacToAndroidKey(macKeycode: Int): Int {
        // Simple mapping for common keys
        return when (macKeycode) {
            10001 -> KeyEvent.KEYCODE_HOME
            10002 -> KeyEvent.KEYCODE_BACK
            10003 -> KeyEvent.KEYCODE_APP_SWITCH
            10004 -> KeyEvent.KEYCODE_NOTIFICATION
            0 -> KeyEvent.KEYCODE_A
            1 -> KeyEvent.KEYCODE_S
            2 -> KeyEvent.KEYCODE_D
            3 -> KeyEvent.KEYCODE_F
            4 -> KeyEvent.KEYCODE_H
            5 -> KeyEvent.KEYCODE_G
            6 -> KeyEvent.KEYCODE_Z
            7 -> KeyEvent.KEYCODE_X
            8 -> KeyEvent.KEYCODE_C
            9 -> KeyEvent.KEYCODE_V
            11 -> KeyEvent.KEYCODE_B
            12 -> KeyEvent.KEYCODE_Q
            13 -> KeyEvent.KEYCODE_W
            14 -> KeyEvent.KEYCODE_E
            15 -> KeyEvent.KEYCODE_R
            16 -> KeyEvent.KEYCODE_Y
            17 -> KeyEvent.KEYCODE_T
            31 -> KeyEvent.KEYCODE_O
            32 -> KeyEvent.KEYCODE_U
            34 -> KeyEvent.KEYCODE_I
            35 -> KeyEvent.KEYCODE_P
            36 -> KeyEvent.KEYCODE_ENTER
            37 -> KeyEvent.KEYCODE_L
            38 -> KeyEvent.KEYCODE_J
            40 -> KeyEvent.KEYCODE_K
            45 -> KeyEvent.KEYCODE_N
            46 -> KeyEvent.KEYCODE_M
            48 -> KeyEvent.KEYCODE_TAB
            49 -> KeyEvent.KEYCODE_SPACE
            51 -> KeyEvent.KEYCODE_DEL // Backspace
            53 -> KeyEvent.KEYCODE_ESCAPE
            123 -> KeyEvent.KEYCODE_DPAD_LEFT
            124 -> KeyEvent.KEYCODE_DPAD_RIGHT
            125 -> KeyEvent.KEYCODE_DPAD_DOWN
            126 -> KeyEvent.KEYCODE_DPAD_UP
            else -> KeyEvent.KEYCODE_UNKNOWN
        }
    }
}
