package com.drift.droiddrift

import android.util.Log
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import org.json.JSONObject
import java.net.InetSocketAddress

class SocketServer(port: Int, private val listener: SocketEventListener) : WebSocketServer(InetSocketAddress(port)) {
    private val TAG = "SocketServer"
    var activeConnection: WebSocket? = null
        private set

    fun hasActiveConnection(): Boolean {
        return activeConnection != null
    }

    fun sendExit() {
        Log.d(TAG, "Sending exit message back to macOS.")
        try {
            activeConnection?.send("{\"type\": \"exit\"}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send exit message: ${e.message}")
        }
    }

    override fun onOpen(conn: WebSocket?, handshake: ClientHandshake?) {
        Log.d(TAG, "New connection accepted from client: ${conn?.remoteSocketAddress}")
        activeConnection = conn
    }

    override fun onClose(conn: WebSocket?, code: Int, reason: String?, remote: Boolean) {
        Log.d(TAG, "Connection closed with client: ${conn?.remoteSocketAddress}")
        if (activeConnection == conn) {
            activeConnection = null
        }
    }

    override fun onMessage(conn: WebSocket?, message: String?) {
        if (message == null) return
        try {
            val json = JSONObject(message)
            val type = json.optString("type")
            
            when (type) {
                "enter" -> {
                    val yRatio = json.optDouble("y_ratio", 0.5).toFloat()
                    listener.onEnter(yRatio)
                }
                "mouse_move" -> {
                    val dx = json.optDouble("dx", 0.0).toFloat()
                    val dy = json.optDouble("dy", 0.0).toFloat()
                    listener.onMouseMove(dx, dy)
                }
                "mouse_button" -> {
                    val button = json.optString("button")
                    val state = json.optString("state")
                    listener.onMouseButton(button, state)
                }
                "keyboard_key" -> {
                    val keycode = json.optInt("keycode")
                    val state = json.optString("state")
                    listener.onKeyboardKey(keycode, state)
                }
                "scroll" -> {
                    val dx = json.optDouble("dx", 0.0).toFloat()
                    val dy = json.optDouble("dy", 0.0).toFloat()
                    listener.onScroll(dx, dy)
                }
                "ping" -> {
                    conn?.send("{\"type\": \"pong\"}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing WebSocket message: ${e.message}")
        }
    }

    override fun onError(conn: WebSocket?, ex: Exception?) {
        Log.e(TAG, "WebSocket server error: ${ex?.message}")
        ex?.printStackTrace()
    }

    override fun onStart() {
        Log.i(TAG, "WebSocket server successfully started on port: $port")
    }
}

interface SocketEventListener {
    fun onEnter(yRatio: Float)
    fun onMouseMove(dx: Float, dy: Float)
    fun onMouseButton(button: String, state: String)
    fun onKeyboardKey(keycode: Int, state: String)
    fun onScroll(dx: Float, dy: Float)
}
