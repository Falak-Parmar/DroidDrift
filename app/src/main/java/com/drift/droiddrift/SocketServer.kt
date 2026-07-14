package com.drift.droiddrift

import android.util.Log
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import org.json.JSONObject
import java.net.InetSocketAddress

class SocketServer(port: Int, private val simulator: InputSimulator) : WebSocketServer(InetSocketAddress(port)) {
    private val TAG = "SocketServer"
    private var activeConnection: WebSocket? = null

    init {
        // Set simulator exit callback to send an exit event back to the macOS transmitter
        simulator.onExitLeft = {
            Log.d(TAG, "Virtual cursor exited left display bounds. Sending exit message back to macOS.")
            activeConnection?.send("{\"type\": \"exit\"}")
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
                    simulator.resetCursor(yRatio)
                }
                "mouse_move" -> {
                    val dx = json.optDouble("dx", 0.0).toFloat()
                    val dy = json.optDouble("dy", 0.0).toFloat()
                    simulator.injectMouseMove(dx, dy)
                }
                "mouse_button" -> {
                    val button = json.optString("button")
                    val state = json.optString("state")
                    simulator.injectMouseButton(button, state)
                }
                "keyboard_key" -> {
                    val keycode = json.optInt("keycode")
                    val state = json.optString("state")
                    simulator.injectKey(keycode, state)
                }
                "scroll" -> {
                    val dx = json.optDouble("dx", 0.0).toFloat()
                    val dy = json.optDouble("dy", 0.0).toFloat()
                    simulator.injectScroll(dx, dy)
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
