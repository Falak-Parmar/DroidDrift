package com.drift.droiddrift

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.graphics.Path
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.net.Socket

class InputAccessibilityService : AccessibilityService(), SocketEventListener {
    companion object {
        var instance: InputAccessibilityService? = null
            private set
    }
    private val TAG = "InputAccessibilityService"
    private val webSocketPort = 8080
    private val adbPort = 9000

    private var server: SocketServer? = null
    private var overlayManager: CursorOverlayManager? = null

    // Screen bounds and coordinates
    private var screenWidth = 1080
    private var screenHeight = 2400
    private var currentX = 500f
    private var currentY = 500f
    private var lastEntryTime = 0L
    private var lastScrollTime = 0L

    // ADB Service Connection
    private var adbSocket: Socket? = null
    private var adbWriter: BufferedWriter? = null
    private var isServiceRunning = false

    override fun onCreate() {
        super.onCreate()
        overlayManager = CursorOverlayManager(this)
        
        // Update screen metrics
        val displayMetrics = resources.displayMetrics
        screenWidth = displayMetrics.widthPixels
        screenHeight = displayMetrics.heightPixels
        Log.d(TAG, "Initialized screen bounds: ${screenWidth}x${screenHeight}")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.i(TAG, "InputAccessibilityService connected.")
        isServiceRunning = true

        // 1. Start WebSocket server to connect with Mac client
        try {
            server = SocketServer(webSocketPort, this)
            server?.isReuseAddr = true
            server?.start()
            Log.d(TAG, "WebSocket server started on port $webSocketPort")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start WebSocket server: ${e.message}")
        }

        // 2. Start AdbMain client listener
        startAdbConnectionThread()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Passive accessibility events are ignored
    }

    override fun onInterrupt() {
        Log.w(TAG, "Service Interrupted.")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        isServiceRunning = false
        
        try {
            server?.stop()
            Log.d(TAG, "WebSocket server stopped.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop WebSocket server: ${e.message}")
        }

        closeAdbSocket()
        overlayManager?.hide()
        Log.i(TAG, "InputAccessibilityService destroyed.")
    }

    // --- WebSocket Event Listeners ---

    override fun onEnter(yRatio: Float) {
        lastEntryTime = SystemClock.uptimeMillis()
        currentX = screenWidth.toFloat() - 30f // Start slightly inside phone screen
        currentY = yRatio * screenHeight
        Log.d(TAG, "Cursor entered at: ($currentX, $currentY)")
        
        if (!isAdbConnected()) {
            overlayManager?.updatePosition(currentX, currentY)
        } else {
            overlayManager?.hide()
        }
        sendAdbCommand("R,$yRatio")
    }

    override fun onMouseMove(dx: Float, dy: Float) {
        val nextX = currentX + dx
        val nextY = (currentY + dy).coerceIn(0f, screenHeight.toFloat())

        // Check for exit boundary crossing (right edge of phone screen)
        if (nextX > screenWidth) {
            currentX = screenWidth.toFloat()
            val timeSinceEntry = SystemClock.uptimeMillis() - lastEntryTime
            if (timeSinceEntry > 500) { // 500ms temporal cooldown
                Log.d(TAG, "Exiting phone screen boundaries back to Mac.")
                server?.sendExit()
                overlayManager?.hide()
                sendAdbCommand("M,$dx,$dy") // Optional command clean exit
            }
            return
        }

        currentX = nextX.coerceIn(0f, screenWidth.toFloat())
        currentY = nextY

        if (!isAdbConnected()) {
            overlayManager?.updatePosition(currentX, currentY)
        } else {
            overlayManager?.hide()
        }

        if (isAdbConnected()) {
            sendAdbCommand("M,$dx,$dy")
        }
    }

    override fun onMouseButton(button: String, state: String) {
        if (isAdbConnected()) {
            sendAdbCommand("B,$button,$state")
        } else {
            // Fallback: Dispatch gesture click on down state
            if (state == "down") {
                dispatchTap(currentX, currentY)
            }
        }
    }

    override fun onKeyboardKey(keycode: Int, state: String) {
        if (state == "down") {
            val globalAction = when (keycode) {
                10001 -> GLOBAL_ACTION_HOME
                10002 -> GLOBAL_ACTION_BACK
                10003 -> GLOBAL_ACTION_RECENTS
                10004 -> GLOBAL_ACTION_NOTIFICATIONS
                else -> -1
            }
            if (globalAction != -1) {
                performGlobalAction(globalAction)
                return
            }
        }
        if (isAdbConnected()) {
            sendAdbCommand("K,$keycode,$state")
        }
    }

    override fun onScroll(dx: Float, dy: Float) {
        if (isAdbConnected()) {
            sendAdbCommand("S,$dx,$dy")
        } else {
            // Fallback: Dispatch scroll gesture with throttled and dampened multipliers (0.35f) to prevent queue clogging!
            val now = SystemClock.uptimeMillis()
            if (now - lastScrollTime >= 300) {
                lastScrollTime = now
                dispatchScroll(dx * 0.35f, dy * 0.35f)
            }
        }
    }

    // --- ADB Connection Manager ---

    private fun startAdbConnectionThread() {
        Thread {
            while (isServiceRunning) {
                if (adbSocket == null || adbSocket!!.isClosed || !adbSocket!!.isConnected) {
                    try {
                        Log.d(TAG, "Connecting to local AdbMain injector daemon on localhost:$adbPort...")
                        val socket = Socket("127.0.0.1", adbPort)
                        adbSocket = socket
                        adbWriter = BufferedWriter(OutputStreamWriter(socket.getOutputStream()))
                        Log.i(TAG, "Connected to AdbMain daemon! Native injection active.")
                    } catch (e: Exception) {
                        // Suppress exception log unless debug
                        Thread.sleep(3000)
                    }
                } else {
                    Thread.sleep(5000)
                }
            }
        }.start()
    }

    private fun sendAdbCommand(cmd: String) {
        val writer = adbWriter
        if (writer != null) {
            try {
                synchronized(writer) {
                    writer.write(cmd)
                    writer.newLine()
                    writer.flush()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send command to ADB daemon: ${e.message}")
                closeAdbSocket()
            }
        }
    }

    fun isAdbConnected(): Boolean {
        val socket = adbSocket
        return socket != null && socket.isConnected && !socket.isClosed
    }

    fun isMacConnected(): Boolean {
        return server?.hasActiveConnection() == true
    }

    private fun closeAdbSocket() {
        try {
            adbWriter?.close()
            adbSocket?.close()
        } catch (e: Exception) {
            // Ignored
        }
        adbWriter = null
        adbSocket = null
    }

    // --- Gesture Fallback Injections ---

    private fun dispatchTap(x: Float, y: Float) {
        val path = Path().apply {
            moveTo(x, y)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 50))
            .build()
        dispatchGesture(gesture, null, null)
    }

    private fun dispatchScroll(dx: Float, dy: Float) {
        val path = Path().apply {
            moveTo(currentX, currentY)
            // Horizontal scroll is natural with (currentX - dx); vertical scroll is natural with (currentY + dy)
            lineTo(currentX - dx, currentY + dy)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 150))
            .build()
        dispatchGesture(gesture, null, null)
    }
}
