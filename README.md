# DroidDrift

**DroidDrift** is the Android companion app for the [Drift](https://github.com/Falak-Parmar/Drift) universal control system. It acts as both a WebSocket server and a privileged input injector, receiving mouse, keyboard, and scroll events forwarded from **AirDrift** on macOS and injecting them natively into Android without root.

---

## Features

- 📡 **WebSocket server** — listens on port `8080` for events from AirDrift
- 🖱️ **Native HID-level mouse injection** — relative mouse movements via the Android `hid` daemon, enabling actual hardware pointer behaviour
- ⌨️ **Keyboard injection** — full key forwarding including Android system keys (Home, Back, Recents, Notifications)
- 📜 **Scroll forwarding** — vertical and horizontal scroll deltas with configurable multiplier
- 🔒 **Privileged ADB daemon** — runs inside the ADB shell context (`UID=shell`) to bypass app sandboxing restrictions, no root required
- ♿ **Accessibility Service fallback** — keeps the connection alive and handles permission bootstrapping
- 🎨 **Mihon-style Appearance Settings** — choose from 5 visual themes: Default, Dynamic, Catppuccin, Nord, OLED
- 🌓 **System theme adaptation** — automatically switches between light/dark based on system settings
- 💊 **Pill-shaped floating bottom navigation** — Profile · Home · Settings
- 🧑‍💻 **Developer profile card** — clickable GitHub link with transparent profile picture

---

## Architecture

```
AirDrift (macOS)
└── WebSocket Client → ws://127.0.0.1:8080
    └── ADB Port Forward: tcp:8080 → tcp:8080
        └── DroidDrift WebSocket Server (port 8080)
            ├── ADB Daemon (app_process / UID=shell)
            │   └── InputFlinger / HID injection
            └── Accessibility Service (fallback)
```

---

## Requirements

- Android 9 (API 28)+
- ADB enabled (**USB Debugging** or **Wireless Debugging**)
- macOS with **AirDrift** installed and connected

---

## Setup & Running

### 1. Build the APK

```bash
cd DroidDrift
./gradlew assembleDebug
```

### 2. Push & Install APK

```bash
adb push app/build/outputs/apk/debug/app-debug.apk /data/local/tmp/
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 3. Grant Accessibility Permission (auto-granted via ADB)

```bash
adb shell settings put secure enabled_accessibility_services \
  com.drift.droiddrift/com.drift.droiddrift.InputAccessibilityService
adb shell settings put secure accessibility_enabled 1
```

### 4. Start the Privileged ADB Daemon

Pass your screen resolution (width × height in pixels):

```bash
adb shell "export CLASSPATH=/data/local/tmp/app-debug.apk; \
  exec app_process /data/local/tmp com.drift.droiddrift.AdbMain 1080 2400"
```

### 5. Forward Port Over USB/Wireless

```bash
adb forward tcp:8080 tcp:8080
```

### 6. Launch the App

```bash
adb shell am start -n com.drift.droiddrift/.MainActivity
```

The app opens on the **Home (Dashboard)** tab showing connection status and permissions checklist.

---

## Visual Themes

Navigate to **Settings → Appearance Settings** to switch themes:

| Theme | Description |
|---|---|
| **Default** | Orange primary, green secondary |
| **Dynamic** | Material blue & cyan |
| **Catppuccin** | Pastel lavender & pink |
| **Nord** | Arctic frost blue & ice |
| **OLED** | Pure black / pure white, AMOLED optimised |

Toggle **Pure black dark mode** to override all dark backgrounds with `#000000` for any theme.

---

## Navigation

The pill-shaped floating bottom nav bar contains three panels:

| Tab | Contents |
|---|---|
| **Profile** | Developer info, GitHub link, app version |
| **Home** | Connection status, WebSocket address, permissions checklist |
| **Settings** | Control tweaks (scroll speed, border width, cooldown) + Appearance Settings |

---

## Developer

Made by [Falak Parmar](https://github.com/Falak-Parmar)  
Part of the [Drift](https://github.com/Falak-Parmar/Drift) project.
