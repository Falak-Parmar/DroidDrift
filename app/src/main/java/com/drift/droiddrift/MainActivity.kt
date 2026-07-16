package com.drift.droiddrift

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.net.NetworkInterface
import java.util.Collections

class MainActivity : ComponentActivity() {
    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Render Glassmorphic Guided UI
        setContent {
            DriftTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun DriftTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF64B5F6),
            secondary = Color(0xFF81C784),
            background = Color(0xFF0F0C1B),
            surface = Color(0xFF1B162B),
            onPrimary = Color.Black,
            onBackground = Color.White,
            onSurface = Color.White
        ),
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    var isAccessibilityEnabled by remember { mutableStateOf(false) }
    var isOverlayEnabled by remember { mutableStateOf(false) }
    var isAdbConnected by remember { mutableStateOf(false) }
    var isMacConnected by remember { mutableStateOf(false) }
    var localIp by remember { mutableStateOf("0.0.0.0") }

    val displayMetrics = context.resources.displayMetrics
    val screenWidth = displayMetrics.widthPixels
    val screenHeight = displayMetrics.heightPixels

    // Flashing dot animation for active connection
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // Reactive Status Loop
    LaunchedEffect(Unit) {
        while (true) {
            isAccessibilityEnabled = isAccessibilityServiceEnabled(context, InputAccessibilityService::class.java)
            isOverlayEnabled = Settings.canDrawOverlays(context)
            isAdbConnected = InputAccessibilityService.instance?.isAdbConnected() == true
            isMacConnected = InputAccessibilityService.instance?.isMacConnected() == true
            localIp = getLocalIpAddress()
            delay(1000)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0C091A), Color(0xFF17112E))
                )
            )
            .padding(20.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
        ) {
            // Header
            Text(
                text = "DroidDrift Hub",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontFamily = FontFamily.SansSerif,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            
            // Connection Status Panel
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0x33FFFFFF)),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(20.dp))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .scale(if (isMacConnected) pulseScale else 1f)
                                .background(
                                    if (isMacConnected) Color(0xFF81C784) else Color(0xFFFFB74D),
                                    RoundedCornerShape(5.dp)
                                )
                        )
                        Text(
                            text = if (isMacConnected) "Active Connection" else "Waiting for macOS",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isMacConnected) Color(0xFF81C784) else Color(0xFFFFB74D)
                        )
                    }

                    Text(
                        text = "ws://$localIp:8080",
                        fontSize = 20.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White
                    )

                    Text(
                        text = "Resolution: ${screenWidth}x${screenHeight}",
                        fontSize = 12.sp,
                        color = Color(0xFFB0BEC5)
                    )
                }
            }

            // Checklist Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0x1EFFFFFF)),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0x10FFFFFF), RoundedCornerShape(20.dp))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "📋 Setup Checklist",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64B5F6)
                    )

                    // 1. Accessibility
                    ChecklistItem(
                        title = "Accessibility Service",
                        desc = "Required to auto-start listener & gesture fallback.",
                        isCompleted = isAccessibilityEnabled,
                        onButtonClick = {
                            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            context.startActivity(intent)
                        }
                    )

                    Divider(color = Color(0x0FFFFFFF), thickness = 1.dp)

                    // 2. Display Over Other Apps
                    ChecklistItem(
                        title = "Display Over Other Apps",
                        desc = "Required to draw floating pointer.",
                        isCompleted = isOverlayEnabled,
                        onButtonClick = {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                        }
                    )

                    Divider(color = Color(0x0FFFFFFF), thickness = 1.dp)

                    // 3. ADB Injection Daemon
                    AdbChecklistItem(
                        isAdbConnected = isAdbConnected,
                        screenWidth = screenWidth,
                        screenHeight = screenHeight
                    )
                }
            }
        }
    }
}

@Composable
fun ChecklistItem(
    title: String,
    desc: String,
    isCompleted: Boolean,
    onButtonClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (isCompleted) Color(0xFF81C784) else Color(0xFFEF9A9A),
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
            }
            Text(
                text = desc,
                fontSize = 12.sp,
                color = Color(0xFFB0BEC5),
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        if (!isCompleted) {
            Button(
                onClick = onButtonClick,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF64B5F6)),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                modifier = Modifier.padding(start = 10.dp)
            ) {
                Text("Grant", fontSize = 12.sp, color = Color.Black)
            }
        }
    }
}

@Composable
fun AdbChecklistItem(isAdbConnected: Boolean, screenWidth: Int, screenHeight: Int) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = if (isAdbConnected) Color(0xFF81C784) else Color(0xFFFFB74D),
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "ADB Injection Daemon",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
        }
        Text(
            text = if (isAdbConnected) "Status: Connected (Native injection active)" else "Status: Disconnected (Accessibility gesture fallback active)",
            fontSize = 12.sp,
            color = if (isAdbConnected) Color(0xFF81C784) else Color(0xFFFFB74D),
            modifier = Modifier.padding(top = 2.dp)
        )

        if (!isAdbConnected) {
            Text(
                text = "To enable lag-free native system-level input injection, run this inside ADB shell:",
                fontSize = 11.sp,
                color = Color(0xFFB0BEC5),
                modifier = Modifier.padding(top = 10.dp, bottom = 6.dp)
            )
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0x33000000),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0x0FFFFFFF), RoundedCornerShape(8.dp))
            ) {
                Text(
                    text = "adb shell \"export CLASSPATH=/data/local/tmp/app-debug.apk; exec app_process /data/local/tmp com.drift.droiddrift.AdbMain $screenWidth $screenHeight\"",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF81C784),
                    modifier = Modifier.padding(8.dp),
                    textAlign = TextAlign.Start
                )
            }
        }
    }
}

fun isAccessibilityServiceEnabled(context: Context, service: Class<out AccessibilityService>): Boolean {
    val expectedComponentName = ComponentName(context, service)
    val enabledServicesSetting = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false
    val colonSplitter = TextUtils.SimpleStringSplitter(':')
    colonSplitter.setString(enabledServicesSetting)
    while (colonSplitter.hasNext()) {
        val componentNameString = colonSplitter.next()
        val enabledService = ComponentName.unflattenFromString(componentNameString)
        if (enabledService != null && enabledService == expectedComponentName) {
            return true
        }
    }
    return false
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
    return "0.0.0.0"
}
