# DroidDrift

DroidDrift is the Android companion app and daemon for the **AirDrift** mouse and keyboard sharing ecosystem. It establishes a WebSocket server, receives mouse/keyboard deltas and state events from macOS, and injects them globally into Android.

## Setup & Running (Privileged ADB Shell Daemon)

Since user-space apps cannot acquire the signature-level `android.permission.INJECT_EVENTS` permission on standard unrooted devices, the event injector server must be run inside the ADB shell context (UID `shell`), which natively has these privileges.

### 1. Push APK to Device
Push the compiled APK to a temporary directory on your phone:
```bash
adb push app/build/outputs/apk/debug/app-debug.apk /data/local/tmp/
```

### 2. Start the Daemon
Execute the server using the Android `app_process` runner inside the ADB shell. Pass your phone's screen resolution (`width` `height`) as arguments:
```bash
adb shell "export CLASSPATH=/data/local/tmp/app-debug.apk; exec app_process /data/local/tmp com.drift.droiddrift.AdbMain 1080 2320"
```

### 3. Forward Port Over USB
Forward port `8080` from your Mac to the phone:
```bash
adb forward tcp:8080 tcp:8080
```

## Features
- Headless, rootless event injection via `ServiceManager` input binder.
- Temporal entry/exit cooldown (500ms) to prevent edge lock loops.
- Support for relative mouse movements, clicks, scrolls, and key presses.
