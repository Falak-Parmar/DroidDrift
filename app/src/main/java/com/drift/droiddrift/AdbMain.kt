package com.drift.droiddrift

import android.os.Looper
import android.util.Log

object AdbMain {
    private const val TAG = "AdbMain"

    @JvmStatic
    fun main(args: Array<String>) {
        println("==================================================")
        println("  DroidDrift ADB Shell Server (Running as Shell)  ")
        println("==================================================")
        
        // Prepare main thread message looper for events
        Looper.prepareMainLooper()
        
        val simulator = InputSimulator()
        
        // Read resolution arguments if provided, otherwise default to standard phone
        var width = 1080
        var height = 2400
        
        if (args.size >= 2) {
            width = args[0].toIntOrNull() ?: 1080
            height = args[1].toIntOrNull() ?: 2400
        }
        
        simulator.screenWidth = width
        simulator.screenHeight = height
        println("Device display bounds: ${width}x${height}")
        
        val port = 8080
        val server = SocketServer(port, simulator)
        server.isReuseAddr = true
        server.start()
        
        println("WebSocket server active on port: $port")
        println("Connection mode: USB Port Forwarding / Wi-Fi")
        println("Press Ctrl+C to terminate.")
        println("==================================================")
        
        Looper.loop()
    }
}
