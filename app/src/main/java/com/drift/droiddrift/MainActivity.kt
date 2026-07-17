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
import coil.compose.AsyncImage
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
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

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

enum class VisualTheme {
    DEFAULT, DYNAMIC, CATPPUCCIN, NORD, OLED
}

enum class Screen {
    PROFILE, HOME, SETTINGS
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val prefs = getSharedPreferences("drift_prefs", Context.MODE_PRIVATE)
        
        // Load initial values from SharedPreferences
        val savedThemeMode = prefs.getString("theme_mode", ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
        val savedVisualTheme = prefs.getString("visual_theme", VisualTheme.DEFAULT.name) ?: VisualTheme.DEFAULT.name
        val savedPureBlack = prefs.getBoolean("pure_black_dark", false)
        val savedTabletUi = prefs.getString("tablet_ui", "Auto") ?: "Auto"
        val savedDateFormat = prefs.getString("date_format", "YYYY-MM-DD") ?: "YYYY-MM-DD"
        val savedRelativeTimestamps = prefs.getBoolean("relative_timestamps", true)
        
        // Dynamic control tweak settings (loaded from preferences, defaults matching macOS app)
        val savedScrollSpeed = prefs.getFloat("tweak_scroll_speed", 1.0f)
        val savedBorderWidth = prefs.getFloat("tweak_border_width", 10.0f)
        val savedCooldown = prefs.getFloat("tweak_cooldown", 0.5f)

        setContent {
            var themeMode by remember { mutableStateOf(try { ThemeMode.valueOf(savedThemeMode) } catch(e: Exception) { ThemeMode.SYSTEM }) }
            var visualTheme by remember { mutableStateOf(try { VisualTheme.valueOf(savedVisualTheme) } catch(e: Exception) { VisualTheme.DEFAULT }) }
            var pureBlackDark by remember { mutableStateOf(savedPureBlack) }
            var tabletUi by remember { mutableStateOf(savedTabletUi) }
            var dateFormat by remember { mutableStateOf(savedDateFormat) }
            var relativeTimestamps by remember { mutableStateOf(savedRelativeTimestamps) }
            
            var scrollSpeed by remember { mutableStateOf(savedScrollSpeed) }
            var borderWidth by remember { mutableStateOf(savedBorderWidth) }
            var cooldown by remember { mutableStateOf(savedCooldown) }

            DriftTheme(
                themeMode = themeMode,
                visualTheme = visualTheme,
                pureBlackDark = pureBlackDark
            ) {
                MainScreen(
                    themeMode = themeMode,
                    visualTheme = visualTheme,
                    pureBlackDark = pureBlackDark,
                    tabletUi = tabletUi,
                    dateFormat = dateFormat,
                    relativeTimestamps = relativeTimestamps,
                    scrollSpeed = scrollSpeed,
                    borderWidth = borderWidth,
                    cooldown = cooldown,
                    onThemeModeChange = { mode ->
                        themeMode = mode
                        prefs.edit().putString("theme_mode", mode.name).apply()
                    },
                    onVisualThemeChange = { theme ->
                        visualTheme = theme
                        prefs.edit().putString("visual_theme", theme.name).apply()
                    },
                    onPureBlackChange = { pure ->
                        pureBlackDark = pure
                        prefs.edit().putBoolean("pure_black_dark", pure).apply()
                    },
                    onTabletUiChange = { ui ->
                        tabletUi = ui
                        prefs.edit().putString("tablet_ui", ui).apply()
                    },
                    onDateFormatChange = { format ->
                        dateFormat = format
                        prefs.edit().putString("date_format", format).apply()
                    },
                    onRelativeTimestampsChange = { relative ->
                        relativeTimestamps = relative
                        prefs.edit().putBoolean("relative_timestamps", relative).apply()
                    },
                    onScrollSpeedChange = { speed ->
                        scrollSpeed = speed
                        prefs.edit().putFloat("tweak_scroll_speed", speed).apply()
                    },
                    onBorderWidthChange = { width ->
                        borderWidth = width
                        prefs.edit().putFloat("tweak_border_width", width).apply()
                    },
                    onCooldownChange = { cool ->
                        cooldown = cool
                        prefs.edit().putFloat("tweak_cooldown", cool).apply()
                    }
                )
            }
        }
    }
}

@Composable
fun DriftTheme(
    themeMode: ThemeMode,
    visualTheme: VisualTheme,
    pureBlackDark: Boolean,
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = when (visualTheme) {
        VisualTheme.DEFAULT -> {
            if (isDark) {
                val bg = if (pureBlackDark) Color(0xFF000000) else Color(0xFF0F1410)
                darkColorScheme(
                    primary = Color(0xFFFF9800), // Orange
                    secondary = Color(0xFF4CAF50), // Green
                    background = bg,
                    surface = Color(0xFF1B241D),
                    onPrimary = Color.Black,
                    onBackground = Color.White,
                    onSurface = Color.White
                )
            } else {
                lightColorScheme(
                    primary = Color(0xFFE65100),
                    secondary = Color(0xFF2E7D32),
                    background = Color(0xFFF4F8F5),
                    surface = Color(0xFFFFFFFF),
                    onPrimary = Color.White,
                    onBackground = Color.Black,
                    onSurface = Color.Black
                )
            }
        }
        VisualTheme.DYNAMIC -> {
            if (isDark) {
                val bg = if (pureBlackDark) Color(0xFF000000) else Color(0xFF12131A)
                darkColorScheme(
                    primary = Color(0xFF90CAF9), // Blue
                    secondary = Color(0xFF80CBC4),
                    background = bg,
                    surface = Color(0xFF1B1C26),
                    onPrimary = Color.Black,
                    onBackground = Color.White,
                    onSurface = Color.White
                )
            } else {
                lightColorScheme(
                    primary = Color(0xFF1976D2),
                    secondary = Color(0xFF00796B),
                    background = Color(0xFFF0F1F9),
                    surface = Color(0xFFFFFFFF),
                    onPrimary = Color.White,
                    onBackground = Color.Black,
                    onSurface = Color.Black
                )
            }
        }
        VisualTheme.CATPPUCCIN -> {
            if (isDark) {
                val bg = if (pureBlackDark) Color(0xFF000000) else Color(0xFF1E1E2E)
                darkColorScheme(
                    primary = Color(0xFFCBA6F7), // Lavender
                    secondary = Color(0xFFF5C2E7), // Flamingo Pink
                    background = bg,
                    surface = Color(0xFF252538),
                    onPrimary = Color.Black,
                    onBackground = Color.White,
                    onSurface = Color.White
                )
            } else {
                lightColorScheme(
                    primary = Color(0xFF8839EF),
                    secondary = Color(0xFFEA76CB),
                    background = Color(0xFFEFF1F5),
                    surface = Color(0xFFFFFFFF),
                    onPrimary = Color.White,
                    onBackground = Color.Black,
                    onSurface = Color.Black
                )
            }
        }
        VisualTheme.NORD -> {
            if (isDark) {
                val bg = if (pureBlackDark) Color(0xFF000000) else Color(0xFF2E3440)
                darkColorScheme(
                    primary = Color(0xFF88C0D0), // Frost Blue
                    secondary = Color(0xFF81A1C1), // Ice Accent
                    background = bg,
                    surface = Color(0xFF3B4252),
                    onPrimary = Color.Black,
                    onBackground = Color.White,
                    onSurface = Color.White
                )
            } else {
                lightColorScheme(
                    primary = Color(0xFF5E81AC),
                    secondary = Color(0xFF81A1C1),
                    background = Color(0xFFECEFF4),
                    surface = Color(0xFFFFFFFF),
                    onPrimary = Color.White,
                    onBackground = Color.Black,
                    onSurface = Color.Black
                )
            }
        }
        VisualTheme.OLED -> {
            if (isDark) {
                darkColorScheme(
                    primary = Color(0xFFFFFFFF),
                    secondary = Color(0xFF888888),
                    background = Color(0xFF000000),
                    surface = Color(0xFF0C0C0C),
                    onPrimary = Color.Black,
                    onBackground = Color.White,
                    onSurface = Color.White
                )
            } else {
                lightColorScheme(
                    primary = Color(0xFF000000),
                    secondary = Color(0xFF888888),
                    background = Color(0xFFFFFFFF),
                    surface = Color(0xFFF7F7F7),
                    onPrimary = Color.White,
                    onBackground = Color.Black,
                    onSurface = Color.Black
                )
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    themeMode: ThemeMode,
    visualTheme: VisualTheme,
    pureBlackDark: Boolean,
    tabletUi: String,
    dateFormat: String,
    relativeTimestamps: Boolean,
    scrollSpeed: Float,
    borderWidth: Float,
    cooldown: Float,
    onThemeModeChange: (ThemeMode) -> Unit,
    onVisualThemeChange: (VisualTheme) -> Unit,
    onPureBlackChange: (Boolean) -> Unit,
    onTabletUiChange: (String) -> Unit,
    onDateFormatChange: (String) -> Unit,
    onRelativeTimestampsChange: (Boolean) -> Unit,
    onScrollSpeedChange: (Float) -> Unit,
    onBorderWidthChange: (Float) -> Unit,
    onCooldownChange: (Float) -> Unit
) {
    val context = LocalContext.current
    val darkTheme = isSystemInDarkTheme()
    
    // Default open on the HOME panel!
    var selectedScreen by remember { mutableStateOf(Screen.HOME) }
    var showAppearanceSettings by remember { mutableStateOf(false) }

    var isAccessibilityEnabled by remember { mutableStateOf(false) }
    var isOverlayEnabled by remember { mutableStateOf(false) }
    var isAdbConnected by remember { mutableStateOf(false) }
    var isMacConnected by remember { mutableStateOf(false) }
    var localIp by remember { mutableStateOf("0.0.0.0") }

    val displayMetrics = context.resources.displayMetrics
    val screenWidth = displayMetrics.widthPixels
    val screenHeight = displayMetrics.heightPixels

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

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

    Scaffold(
        bottomBar = {
            if (!showAppearanceSettings) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Custom Pill-shaped Floating Bottom Navigation Bar (3 items: Profile, Home, Settings)
                    Surface(
                        shape = RoundedCornerShape(30.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                        tonalElevation = 6.dp,
                        modifier = Modifier
                            .width(280.dp)
                            .border(
                                width = 1.dp,
                                color = if (themeMode == ThemeMode.DARK || (themeMode == ThemeMode.SYSTEM && darkTheme)) Color(0xFF2C2C2C) else Color(0xFFE5E5E7),
                                shape = RoundedCornerShape(30.dp)
                            )
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            FloatingBottomNavItem(
                                selected = selectedScreen == Screen.PROFILE,
                                onClick = { selectedScreen = Screen.PROFILE },
                                icon = Icons.Default.Person,
                                label = "Profile"
                            )
                            FloatingBottomNavItem(
                                selected = selectedScreen == Screen.HOME,
                                onClick = { selectedScreen = Screen.HOME },
                                icon = Icons.Default.Home,
                                label = "Home"
                            )
                            FloatingBottomNavItem(
                                selected = selectedScreen == Screen.SETTINGS,
                                onClick = { selectedScreen = Screen.SETTINGS },
                                icon = Icons.Default.Settings,
                                label = "Settings"
                            )
                        }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (showAppearanceSettings) {
                AppearanceSettingsScreen(
                    themeMode = themeMode,
                    visualTheme = visualTheme,
                    pureBlackDark = pureBlackDark,
                    tabletUi = tabletUi,
                    dateFormat = dateFormat,
                    relativeTimestamps = relativeTimestamps,
                    onThemeModeChange = onThemeModeChange,
                    onVisualThemeChange = onVisualThemeChange,
                    onPureBlackChange = onPureBlackChange,
                    onTabletUiChange = onTabletUiChange,
                    onDateFormatChange = onDateFormatChange,
                    onRelativeTimestampsChange = onRelativeTimestampsChange,
                    onBackClick = { showAppearanceSettings = false }
                )
            } else {
                Box(modifier = Modifier.padding(24.dp)) {
                    when (selectedScreen) {
                        Screen.PROFILE -> AboutTab(darkTheme = darkTheme)
                        Screen.HOME -> DashboardTab(
                            screenWidth = screenWidth,
                            screenHeight = screenHeight,
                            isMacConnected = isMacConnected,
                            pulseAlpha = pulseAlpha,
                            localIp = localIp,
                            isAccessibilityEnabled = isAccessibilityEnabled,
                            isOverlayEnabled = isOverlayEnabled,
                            isAdbConnected = isAdbConnected,
                            darkTheme = darkTheme
                        )
                        Screen.SETTINGS -> SettingsTab(
                            scrollSpeed = scrollSpeed,
                            borderWidth = borderWidth,
                            cooldown = cooldown,
                            onScrollSpeedChange = onScrollSpeedChange,
                            onBorderWidthChange = onBorderWidthChange,
                            onCooldownChange = onCooldownChange,
                            onAppearanceClick = { showAppearanceSettings = true },
                            darkTheme = darkTheme
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FloatingBottomNavItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(20.dp)
        )
        if (selected) {
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettingsScreen(
    themeMode: ThemeMode,
    visualTheme: VisualTheme,
    pureBlackDark: Boolean,
    tabletUi: String,
    dateFormat: String,
    relativeTimestamps: Boolean,
    onThemeModeChange: (ThemeMode) -> Unit,
    onVisualThemeChange: (VisualTheme) -> Unit,
    onPureBlackChange: (Boolean) -> Unit,
    onTabletUiChange: (String) -> Unit,
    onDateFormatChange: (String) -> Unit,
    onRelativeTimestampsChange: (Boolean) -> Unit,
    onBackClick: () -> Unit
) {
    val darkTheme = isSystemInDarkTheme()
    val isThemeDark = when (themeMode) {
        ThemeMode.SYSTEM -> darkTheme
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // 1. Top App Bar
        TopAppBar(
            title = { Text("Appearance", fontSize = 20.sp, fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        )

        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 2. Theme Mode Selector (Segmented buttons style)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Theme mode",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = if (isThemeDark) Color(0xFF2C2C2C) else Color(0xFFE5E5E7),
                            shape = RoundedCornerShape(10.dp)
                        )
                ) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            label = "System",
                            selected = themeMode == ThemeMode.SYSTEM,
                            onClick = { onThemeModeChange(ThemeMode.SYSTEM) },
                            modifier = Modifier.weight(1f)
                        )
                        SegmentedButton(
                            label = "Light",
                            selected = themeMode == ThemeMode.LIGHT,
                            onClick = { onThemeModeChange(ThemeMode.LIGHT) },
                            modifier = Modifier.weight(1f)
                        )
                        SegmentedButton(
                            label = "Dark",
                            selected = themeMode == ThemeMode.DARK,
                            onClick = { onThemeModeChange(ThemeMode.DARK) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 3. Visual Theme Carousel (Mini Phone Mockups)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Visual theme",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    ThemeMockupCard(
                        name = "Default",
                        accent = Color(0xFFFF9800),
                        secAccent = Color(0xFF4CAF50),
                        isSelected = visualTheme == VisualTheme.DEFAULT,
                        isThemeDark = isThemeDark,
                        onClick = { onVisualThemeChange(VisualTheme.DEFAULT) }
                    )
                    ThemeMockupCard(
                        name = "Dynamic",
                        accent = Color(0xFF2196F3),
                        secAccent = Color(0xFF00BCD4),
                        isSelected = visualTheme == VisualTheme.DYNAMIC,
                        isThemeDark = isThemeDark,
                        onClick = { onVisualThemeChange(VisualTheme.DYNAMIC) }
                    )
                    ThemeMockupCard(
                        name = "Catppuccin",
                        accent = Color(0xFFCBA6F7),
                        secAccent = Color(0xFFF5C2E7),
                        isSelected = visualTheme == VisualTheme.CATPPUCCIN,
                        isThemeDark = isThemeDark,
                        onClick = { onVisualThemeChange(VisualTheme.CATPPUCCIN) }
                    )
                    ThemeMockupCard(
                        name = "Nord",
                        accent = Color(0xFF88C0D0),
                        secAccent = Color(0xFF81A1C1),
                        isSelected = visualTheme == VisualTheme.NORD,
                        isThemeDark = isThemeDark,
                        onClick = { onVisualThemeChange(VisualTheme.NORD) }
                    )
                    ThemeMockupCard(
                        name = "OLED",
                        accent = Color(0xFFFFFFFF),
                        secAccent = Color(0xFF888888),
                        isSelected = visualTheme == VisualTheme.OLED,
                        isThemeDark = isThemeDark,
                        onClick = { onVisualThemeChange(VisualTheme.OLED) }
                    )
                }
            }

            // 4. Settings List
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = if (isThemeDark) Color(0xFF2C2C2C) else Color(0xFFE5E5E7),
                        shape = RoundedCornerShape(12.dp)
                    )
            ) {
                Column(modifier = Modifier.padding(6.dp)) {
                    SwitchListItem(
                        title = "Pure black dark mode",
                        description = "Use pitch black background for OLED screens",
                        checked = pureBlackDark,
                        onCheckedChange = onPureBlackChange
                    )
                    
                    Divider(color = if (isThemeDark) Color(0xFF2C2C2C) else Color(0xFFE5E5E7))
                    
                    ClickableListItem(
                        title = "Display",
                        onClick = { /* navigate display details */ }
                    )
                    
                    Divider(color = if (isThemeDark) Color(0xFF2C2C2C) else Color(0xFFE5E5E7))
                    
                    ClickableListItem(
                        title = "App language",
                        onClick = { /* language options */ }
                    )
                    
                    Divider(color = if (isThemeDark) Color(0xFF2C2C2C) else Color(0xFFE5E5E7))
                    
                    ClickableListItemWithSubtitle(
                        title = "Tablet UI",
                        subtitle = tabletUi,
                        onClick = {
                            val nextUi = if (tabletUi == "Auto") "Enabled" else if (tabletUi == "Enabled") "Disabled" else "Auto"
                            onTabletUiChange(nextUi)
                        }
                    )
                    
                    Divider(color = if (isThemeDark) Color(0xFF2C2C2C) else Color(0xFFE5E5E7))
                    
                    ClickableListItemWithSubtitle(
                        title = "Date format",
                        subtitle = dateFormat,
                        onClick = {
                            val nextFormat = if (dateFormat == "YYYY-MM-DD") "MM/DD/YYYY" else if (dateFormat == "MM/DD/YYYY") "DD-MM-YYYY" else "YYYY-MM-DD"
                            onDateFormatChange(nextFormat)
                        }
                    )
                    
                    Divider(color = if (isThemeDark) Color(0xFF2C2C2C) else Color(0xFFE5E5E7))
                    
                    SwitchListItem(
                        title = "Relative timestamps",
                        description = "Show date timestamps relative to current time",
                        checked = relativeTimestamps,
                        onCheckedChange = onRelativeTimestampsChange
                    )
                }
            }
        }
    }
}

@Composable
fun SegmentedButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 12.dp)
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun ThemeMockupCard(
    name: String,
    accent: Color,
    secAccent: Color,
    isSelected: Boolean,
    isThemeDark: Boolean,
    onClick: () -> Unit
) {
    val phoneBg = if (name == "OLED" && isThemeDark) {
        Color.Black
    } else if (isThemeDark) {
        Color(0xFF1E1E2E)
    } else {
        Color(0xFFF3F3F5)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(width = 65.dp, height = 110.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(phoneBg)
                .border(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp)
                )
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .background(accent.copy(alpha = 0.8f))
                )
                
                Spacer(modifier = Modifier.height(10.dp))
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(horizontal = 6.dp)
                ) {
                    Box(modifier = Modifier.size(6.dp).clip(RoundedCornerShape(3.dp)).background(accent))
                    Box(modifier = Modifier.size(6.dp).clip(RoundedCornerShape(3.dp)).background(secAccent))
                    Box(modifier = Modifier.size(6.dp).clip(RoundedCornerShape(3.dp)).background(Color.Gray.copy(alpha = 0.5f)))
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Box(modifier = Modifier.padding(horizontal = 6.dp).width(35.dp).height(4.dp).background(accent.copy(alpha = 0.3f)))
                Spacer(modifier = Modifier.height(4.dp))
                Box(modifier = Modifier.padding(horizontal = 6.dp).width(45.dp).height(4.dp).background(Color.Gray.copy(alpha = 0.2f)))
                
                Spacer(modifier = Modifier.weight(1f))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .background(secAccent.copy(alpha = 0.3f))
                )
            }
        }
        
        Text(
            text = name,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

@Composable
fun SwitchListItem(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
            Text(description, fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
            )
        )
    }
}

@Composable
fun ClickableListItem(
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
    }
}

@Composable
fun ClickableListItemWithSubtitle(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(top = 2.dp))
        }
        Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
    }
}

@Composable
fun SettingsTab(
    scrollSpeed: Float,
    borderWidth: Float,
    cooldown: Float,
    onScrollSpeedChange: (Float) -> Unit,
    onBorderWidthChange: (Float) -> Unit,
    onCooldownChange: (Float) -> Unit,
    onAppearanceClick: () -> Unit,
    darkTheme: Boolean
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Settings",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        // Control Tweaks (Mac App style sliders)
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, if (darkTheme) Color(0xFF2C2C2C) else Color(0xFFE5E5E7), RoundedCornerShape(12.dp))
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Control Tweaks",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                
                // Scroll Speed
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Scroll Speed", fontSize = 13.sp, color = MaterialTheme.colorScheme.onBackground)
                        Text(String.format("%.1fx", scrollSpeed), fontSize = 13.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.secondary)
                    }
                    Slider(
                        value = scrollSpeed,
                        onValueChange = onScrollSpeedChange,
                        valueRange = 0.2f..2.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
                
                Divider(color = if (darkTheme) Color(0xFF2C2C2C) else Color(0xFFE5E5E7))
                
                // Border Locking Width
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Border Locking Width", fontSize = 13.sp, color = MaterialTheme.colorScheme.onBackground)
                        Text(String.format("%.0f px", borderWidth), fontSize = 13.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.secondary)
                    }
                    Slider(
                        value = borderWidth,
                        onValueChange = onBorderWidthChange,
                        valueRange = 2f..25f,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
                
                Divider(color = if (darkTheme) Color(0xFF2C2C2C) else Color(0xFFE5E5E7))
                
                // Re-entry Cooldown
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Screen Re-entry Cooldown", fontSize = 13.sp, color = MaterialTheme.colorScheme.onBackground)
                        Text(String.format("%.1f s", cooldown), fontSize = 13.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.secondary)
                    }
                    Slider(
                        value = cooldown,
                        onValueChange = onCooldownChange,
                        valueRange = 0.1f..1.5f,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        }
        
        // Appearance Settings Entry
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, if (darkTheme) Color(0xFF2C2C2C) else Color(0xFFE5E5E7), RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(6.dp)) {
                ClickableListItem(
                    title = "Appearance Settings",
                    onClick = onAppearanceClick
                )
            }
        }
    }
}

@Composable
fun DashboardTab(
    screenWidth: Int,
    screenHeight: Int,
    isMacConnected: Boolean,
    pulseAlpha: Float,
    localIp: String,
    isAccessibilityEnabled: Boolean,
    isOverlayEnabled: Boolean,
    isAdbConnected: Boolean,
    darkTheme: Boolean
) {
    val context = LocalContext.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // Header
        Text(
            text = "Drift Hub",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            fontFamily = FontFamily.SansSerif,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Start
        )
        
        // Connection Status Panel
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, if (darkTheme) Color(0xFF2C2C2C) else Color(0xFFE5E5E7), RoundedCornerShape(12.dp))
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                if (isMacConnected) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = pulseAlpha)
                                } else {
                                    if (darkTheme) Color(0xFF555555) else Color(0xFFCCCCCC)
                                },
                                RoundedCornerShape(4.dp)
                            )
                    )
                    Text(
                        text = if (isMacConnected) "Active" else "Idle",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isMacConnected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.secondary
                    )
                }

                Text(
                    text = "ws://$localIp:8080",
                    fontSize = 18.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Medium
                )

                Text(
                    text = "Display boundary: ${screenWidth}x${screenHeight}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }

        // Checklist Card
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, if (darkTheme) Color(0xFF2C2C2C) else Color(0xFFE5E5E7), RoundedCornerShape(12.dp))
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Requirements",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                ChecklistItem(
                    title = "Accessibility Service",
                    desc = "Needed to listen for input connections.",
                    isCompleted = isAccessibilityEnabled,
                    onButtonClick = {
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        context.startActivity(intent)
                    }
                )

                Divider(color = if (darkTheme) Color(0xFF2C2C2C) else Color(0xFFE5E5E7), thickness = 1.dp)

                ChecklistItem(
                    title = "Overlay Drawing Permission",
                    desc = "Needed to draw custom overlay controls.",
                    isCompleted = isOverlayEnabled,
                    onButtonClick = {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                        context.startActivity(intent)
                    }
                )

                Divider(color = if (darkTheme) Color(0xFF2C2C2C) else Color(0xFFE5E5E7), thickness = 1.dp)

                AdbChecklistItem(
                    isAdbConnected = isAdbConnected,
                    screenWidth = screenWidth,
                    screenHeight = screenHeight
                )
            }
        }
    }
}

@Composable
fun AboutTab(darkTheme: Boolean) {
    val context = LocalContext.current
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Profile",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, if (darkTheme) Color(0xFF2C2C2C) else Color(0xFFE5E5E7), RoundedCornerShape(12.dp))
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Local transparent profile picture
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = R.drawable.my_notion_face_transparent),
                    contentDescription = "Falak Parmar's Profile Picture",
                    modifier = Modifier
                        .size(80.dp)
                )
                
                Text(
                    text = "Falak Parmar",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.clickable {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Falak-Parmar"))
                        context.startActivity(intent)
                    }
                )
                
                Text(
                    text = "https://github.com/Falak-Parmar",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Falak-Parmar"))
                        context.startActivity(intent)
                    }
                )
                
                Divider(color = if (darkTheme) Color(0xFF2C2C2C) else Color(0xFFE5E5E7), thickness = 1.dp)
                
                Text(
                    text = "Drift Hub v1.0.0",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.secondary
                )
                
                Text(
                    text = "A universal control service linking macOS mouse, keyboard, and scroll inputs to Android virtual device nodes over a secure local ADB tunnel.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.secondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )
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
    val darkTheme = isSystemInDarkTheme()
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = if (isCompleted) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        if (darkTheme) Color(0xFF444444) else Color(0xFFCCCCCC)
                    },
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                text = desc,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        if (!isCompleted) {
            Button(
                onClick = onButtonClick,
                shape = RoundedCornerShape(4.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.padding(start = 10.dp)
            ) {
                Text("Grant", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun AdbChecklistItem(isAdbConnected: Boolean, screenWidth: Int, screenHeight: Int) {
    val darkTheme = isSystemInDarkTheme()
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = if (isAdbConnected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    if (darkTheme) Color(0xFF444444) else Color(0xFFCCCCCC)
                },
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "ADB Injection Daemon",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        Text(
            text = if (isAdbConnected) "Connected (Native USB injection active)" else "Not connected (Ready for ADB launch)",
            fontSize = 12.sp,
            color = if (isAdbConnected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(top = 2.dp)
        )

        if (!isAdbConnected) {
            Text(
                text = "To enable native input injection, start the app and run this shell command on your computer:",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = 8.dp, bottom = 6.dp)
            )
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (darkTheme) Color(0xFF0A0A0A) else Color(0xFFF2F2F7),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, if (darkTheme) Color(0xFF2C2C2C) else Color(0xFFE5E5E7), RoundedCornerShape(6.dp))
            ) {
                Text(
                    text = "adb shell \"export CLASSPATH=/data/local/tmp/app-debug.apk; exec app_process /data/local/tmp com.drift.droiddrift.AdbMain $screenWidth $screenHeight\"",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    color = if (darkTheme) Color(0xFFCCCCCC) else Color(0xFF333333),
                    modifier = Modifier.padding(10.dp),
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
