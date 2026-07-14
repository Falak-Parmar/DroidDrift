package com.drift.droiddrift

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.net.NetworkInterface
import java.util.Collections

class MainActivity : ComponentActivity() {
    private val TAG = "MainActivity"
    private lateinit var simulator: InputSimulator
    private lateinit var server: SocketServer
    private val port = 8080

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 1. Initialize Simulator
        simulator = InputSimulator()
        
        // 2. Query and set screen dimensions
        val displayMetrics = resources.displayMetrics
        simulator.screenWidth = displayMetrics.widthPixels
        simulator.screenHeight = displayMetrics.heightPixels
        Log.d(TAG, "Screen resolution set to: ${simulator.screenWidth}x${simulator.screenHeight}")
        
        // 3. Start Socket Server in background
        try {
            server = SocketServer(port, simulator)
            server.isReuseAddr = true
            server.start()
            Log.d(TAG, "Socket Server initialized on port $port")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start socket server: ${e.message}")
        }
        
        // 4. Get Local IP Address
        val localIp = getLocalIpAddress()
        
        // 5. Render Compose UI
        setContent {
            DriftTheme {
                MainScreen(
                    ipAddress = localIp,
                    port = port,
                    screenWidth = simulator.screenWidth,
                    screenHeight = simulator.screenHeight
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            server.stop()
            Log.d(TAG, "Socket Server stopped.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop socket server: ${e.message}")
        }
    }

    private fun getLocalIpAddress(): String {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                val addrs = Collections.list(intf.inetAddresses)
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress) {
                        val sAddr = addr.hostAddress ?: ""
                        val isIPv4 = sAddr.indexOf(':') < 0
                        if (isIPv4) {
                            return sAddr
                        }
                    }
                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return "Unknown IP"
    }
}

@Composable
fun DriftTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF64B5F6),
            secondary = Color(0xFF81C784),
            background = Color(0xFF121212),
            surface = Color(0xFF1E1E1E),
            onPrimary = Color.Black,
            onBackground = Color.White,
            onSurface = Color.White
        ),
        content = content
    )
}

@Composable
fun MainScreen(ipAddress: String, port: Int, screenWidth: Int, screenHeight: Int) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF121212), Color(0xFF1A1A24))
                )
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "DroidDrift Hub",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontFamily = FontFamily.SansSerif,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // Status Indicator Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E28)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(Color(0xFF81C784), RoundedCornerShape(6.dp))
                        )
                        Text(
                            text = "Server Running",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF81C784)
                        )
                    }
                    Text(
                        text = "Listening on: ws://$ipAddress:$port",
                        fontSize = 16.sp,
                        color = Color(0xFFE0E0E0),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Device Resolution: ${screenWidth}x${screenHeight}",
                        fontSize = 12.sp,
                        color = Color(0xFF9E9E9E)
                    )
                }
            }
            
            // Instruction Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF262636)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "🚀 macOS Connection Guide",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64B5F6)
                    )
                    Text(
                        text = "Open terminal on your Mac and run:",
                        fontSize = 14.sp,
                        color = Color(0xFFB0BEC5)
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF121212),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "swift run AirDrift $ipAddress",
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF81C784),
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
            
            // Permissions Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2E2424)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "🔑 Required Permission",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFEF9A9A)
                    )
                    Text(
                        text = "For Android to process virtual mouse pointers globally, run this shell command via ADB:",
                        fontSize = 13.sp,
                        color = Color(0xFFCFD8DC)
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF1A1212),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "adb shell pm grant com.drift.droiddrift android.permission.INJECT_EVENTS",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFFEF9A9A),
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        }
    }
}
