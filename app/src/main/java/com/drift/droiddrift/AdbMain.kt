package com.drift.droiddrift

import android.os.Looper
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.ServerSocket
import java.net.Socket

object AdbMain {
    private const val PORT = 9000
    private const val TAG = "AdbMain"

    @JvmStatic
    fun main(args: Array<String>) {
        println("==================================================")
        println("  DroidDrift Privileged ADB Daemon (Port: $PORT)  ")
        println("==================================================")

        // Prepare main thread message looper for events
        Looper.prepareMainLooper()

        val simulator = InputSimulator()

        var width = 1080
        var height = 2400

        if (args.size >= 2) {
            width = args[0].toIntOrNull() ?: 1080
            height = args[1].toIntOrNull() ?: 2400
        }

        simulator.screenWidth = width
        simulator.screenHeight = height
        println("Display boundaries: ${width}x${height}")

        val hidMouseManager = HidMouseManager()
        hidMouseManager.start()

        val serverSocket = ServerSocket(PORT)
        println("Daemon active. Waiting for DroidDrift app to connect on localhost:$PORT...")

        Thread {
            while (true) {
                try {
                    val clientSocket = serverSocket.accept()
                    println("App connected successfully! Native injection pipeline active.")
                    // Handle each client connection in a separate thread to prevent socket blocking
                    Thread {
                        handleClientConnection(clientSocket, simulator, hidMouseManager)
                    }.start()
                } catch (e: Exception) {
                    println("Error accepting client connection: ${e.message}")
                }
            }
        }.start()

        Looper.loop()
    }

    private fun handleClientConnection(socket: Socket, simulator: InputSimulator, hid: HidMouseManager) {
        try {
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            while (true) {
                val line = reader.readLine() ?: break
                val parts = line.split(",")
                if (parts.isEmpty()) continue

                when (parts[0]) {
                    "M" -> { // Mouse Move (dx, dy)
                        val dx = parts.getOrNull(1)?.toFloatOrNull() ?: 0f
                        val dy = parts.getOrNull(2)?.toFloatOrNull() ?: 0f
                        
                        // Use native HID mouse relative movement
                        hid.sendMove(dx.toInt(), dy.toInt())
                    }
                    "B" -> { // Mouse Button
                        val button = parts.getOrNull(1) ?: "left"
                        val state = parts.getOrNull(2) ?: "up"
                        hid.sendButton(button, state)
                    }
                    "K" -> { // Keyboard Key
                        val keycode = parts.getOrNull(1)?.toIntOrNull() ?: 0
                        val state = parts.getOrNull(2) ?: "up"
                        simulator.injectKey(keycode, state)
                    }
                    "R" -> { // Reset Cursor (absolute snap on entry)
                        val yRatio = parts.getOrNull(1)?.toFloatOrNull() ?: 0.5f
                        simulator.resetCursor(yRatio)
                        // Wake up/draw native cursor instantly by sending a tiny physical HID relative nudge
                        hid.sendMove(0, 1)
                        hid.sendMove(0, -1)
                    }
                    "S" -> { // Scroll (dx, dy)
                        val dx = parts.getOrNull(1)?.toFloatOrNull() ?: 0f
                        val dy = parts.getOrNull(2)?.toFloatOrNull() ?: 0f
                        hid.sendScroll(dx.toInt(), dy.toInt())
                    }
                }
            }
        } catch (e: java.io.IOException) {
            println("Connection closed: ${e.message}")
        } finally {
            try {
                socket.close()
            } catch (e: Exception) {
                // Ignored
            }
            println("App disconnected. Waiting for reconnection...")
        }
    }

    class HidMouseManager {
        private var process: Process? = null
        private var writer: BufferedWriter? = null
        private var buttonsState = 0

        // Float accumulators to prevent slow trackpad scroll ticks from truncating to zero
        private var scrollAccumulatorV = 0f
        private var scrollAccumulatorH = 0f

        fun isAlive(): Boolean {
            val p = process ?: return false
            return try {
                p.exitValue()
                false
            } catch (e: IllegalThreadStateException) {
                true
            }
        }

        fun start() {
            try {
                if (isAlive()) {
                    return // Keep running the active process
                }

                stop()
                // Sleep 200ms to allow kernel time to release /dev/uhid device nodes
                Thread.sleep(200)

                process = Runtime.getRuntime().exec(arrayOf("hid", "-"))
                writer = BufferedWriter(OutputStreamWriter(process!!.outputStream))

                // Consume stdout in a background thread to prevent buffer clogging
                Thread {
                    try {
                        val stream = process!!.inputStream
                        val buffer = ByteArray(1024)
                        while (stream.read(buffer) != -1) {}
                    } catch (e: Exception) {}
                }.start()

                // Consume stderr in a background thread to print errors to ADB console
                Thread {
                    try {
                        val stream = process!!.errorStream
                        val buffer = ByteArray(1024)
                        var length: Int
                        while (stream.read(buffer).also { length = it } != -1) {
                            System.err.write(buffer, 0, length)
                        }
                    } catch (e: Exception) {}
                }.start()
                
                // Register standard USB mouse descriptor with vertical wheel and AC Pan wheel (horizontal)
                val registerJson = """
                {
                  "id": 1,
                  "command": "register",
                  "name": "AirDriftMouse",
                  "vid": 4660,
                  "pid": 22136,
                  "bus": "usb",
                  "descriptor": [5, 1, 9, 2, 161, 1, 9, 1, 161, 0, 5, 9, 25, 1, 41, 3, 21, 0, 37, 1, 149, 3, 117, 1, 129, 2, 149, 1, 117, 5, 129, 1, 5, 1, 9, 48, 9, 49, 21, 129, 37, 127, 117, 8, 149, 2, 129, 6, 9, 56, 21, 129, 37, 127, 117, 8, 149, 1, 129, 6, 5, 12, 10, 56, 2, 21, 129, 37, 127, 117, 8, 149, 1, 129, 6, 192, 192]
                }
                """.trimIndent().replace("\n", "")
                
                writer?.write(registerJson)
                writer?.newLine()
                writer?.flush()
                println("Native USB HID Mouse registered successfully via /system/bin/hid.")
            } catch (e: Exception) {
                System.err.println("Failed to start hid virtual mouse: ${e.message}")
            }
        }

        fun sendMove(dx: Int, dy: Int) {
            sendReport(buttonsState, dx, dy, 0, 0)
        }

        fun sendButton(button: String, state: String) {
            val bit = when (button) {
                "left" -> 1
                "right" -> 2
                "middle" -> 4
                else -> 0
            }
            buttonsState = if (state == "down") {
                buttonsState or bit
            } else {
                buttonsState and bit.inv()
            }
            sendReport(buttonsState, 0, 0, 0, 0)
        }

        fun sendScroll(hscroll: Int, vscroll: Int) {
            // Accumulate dampened floats to prevent small movements truncating to 0
            scrollAccumulatorV += -vscroll * 0.4f
            scrollAccumulatorH += -hscroll * 0.4f

            val stepV = scrollAccumulatorV.toInt()
            val stepH = scrollAccumulatorH.toInt()

            if (stepV != 0 || stepH != 0) {
                scrollAccumulatorV -= stepV
                scrollAccumulatorH -= stepH
                sendReport(buttonsState, 0, 0, stepV, stepH)
            }
        }

        private fun sendReport(buttons: Int, dx: Int, dy: Int, vscroll: Int, hscroll: Int) {
            val clampDx = dx.coerceIn(-127, 127)
            val clampDy = dy.coerceIn(-127, 127)
            val clampV = vscroll.coerceIn(-127, 127)
            val clampH = hscroll.coerceIn(-127, 127)

            // Convert signed bytes (-127 to 127) to unsigned values (0 to 255)
            val uDx = if (clampDx < 0) clampDx + 256 else clampDx
            val uDy = if (clampDy < 0) clampDy + 256 else clampDy
            val uV = if (clampV < 0) clampV + 256 else clampV
            val uH = if (clampH < 0) clampH + 256 else clampH
            
            val reportJson = """
            {"id": 1, "command": "report", "report": [$buttons, $uDx, $uDy, $uV, $uH]}
            """.trimIndent().replace("\n", "")
            
            try {
                val w = writer
                if (w == null || !isAlive()) {
                    start()
                }
                writer?.write(reportJson)
                writer?.newLine()
                writer?.flush()
            } catch (e: Exception) {
                System.err.println("Failed to send HID report: ${e.message}. Restarting HID daemon...")
                start()
            }
        }

        fun stop() {
            try {
                writer?.close()
            } catch (e: Exception) {}
            try {
                process?.destroy()
            } catch (e: Exception) {}
            writer = null
            process = null
        }
    }
}
