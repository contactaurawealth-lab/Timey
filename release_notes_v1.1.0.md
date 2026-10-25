# Timey v1.1.0 — Native Android Focus & Study Timer

A major production release introducing the **135:15 Deep Work study cycle**, native **Accessibility App Blocker**, **Persistent Background Service & Guaranteed Alarm Ringing**, **Fullscreen Focus Mode**, **7 Offline Procedural Soundscapes & MP3 player**, and **Room Database Analytics**.

---

### 📦 Downloads & Verification

| Asset | Size | Format | SHA-256 Checksum |
| :--- | :--- | :--- | :--- |
| **`Timey-v1.1.0.apk`** | 11.9 MB | Signed APK | `6efd3c85e45b46ed1692d13c24152903f81e7cf8267b773fc82f1b771378bbaa` |

- **Minimum SDK**: Android 8.0 (API 26)
- **Target SDK**: Android 15 (API 35)
- **Architectures**: Universal (`arm64-v8a`, `armeabi-v7a`, `x86_64`)
- **Compatibility**: Samsung One UI, Xiaomi HyperOS/MIUI, Pixel, OnePlus OxygenOS, Moto

---

### 🚀 What's New in v1.1.0

#### 1. ⏱️ 135:15 Deep Work Study Cycle & Custom Intervals
- Added scientific **135:15 Deep Work Cycle** (135 min immersion + 15 min rest) tailored for university coursework, coding, and mathematical problem-solving.
- Classic Pomodoro (25:5 / 50:10) with automatic long breaks.
- Custom study & break interval sliders with instant live updates.
- Phase skipping, pause, resume, and accidental exit safeguards.

#### 2. 🔊 Guaranteed Alarm Ringing & Unkillable Background Operation
- **`AlarmRingingService`**: Runs on Android's hardware `USAGE_ALARM` audio stream with `FLAG_AUDIBILITY_ENFORCED`, overriding silent modes and volume toggles so you never miss the end of a study session.
- **Continuous Ringing**: Continuous looping alarm audio accompanied by a synchronized hardware vibration pattern and full-screen lock screen notification.
- **`START_STICKY` Resilience**: `FocusForegroundService` runs with `START_STICKY` and an active 8-hour partial `WakeLock`, preventing OEM battery management from terminating the timer during sleep.

#### 3. 🛡️ Focus Shield (Android App Blocker)
- Native `AccessibilityService` (`FocusAccessibilityBlockerService`) intercepting window state transitions (`TYPE_WINDOW_STATE_CHANGED`).
- Blocks distraction apps with an instant fullscreen overlay showing remaining session time and topic.
- Preset distraction suites (Social, Gaming, Entertainment, Streaming) and custom installed app picker.
- PIN protection to prevent impulsively closing or bypassing the focus session.

#### 4. 🧘 Immersive Fullscreen Focus Mode
- Distraction-free full-screen interface using `WindowInsetsControllerCompat` to completely hide navigation and status bars.
- Minimalist circular countdown timer, ambient quotes, and single-tap controls.
- Prevent accidental termination with confirmation dialogs or PIN lock.

#### 5. 🎧 7 Procedural Soundscapes & Custom MP3 Player
- **100% Offline Procedural Sound Generation** (zero external assets or network required):
  1. *Rain & Thunder*
  2. *Forest Birds*
  3. *10Hz Binaural Alpha Waves*
  4. *Brown Noise*
  5. *White Noise*
  6. *Campfire Hearth*
  7. *Lo-Fi Chill Pulse*
- **Personal Audio**: Import local MP3 files via Storage Access Framework (SAF) with loop (single/all) and shuffle modes.
- Independent music vs. alarm notification volume sliders.

#### 6. 📊 Room Database Persistence & Analytics
- Complete local Room persistence (`StudySessionEntity`, `StudySessionDao`).
- Daily/weekly study time tracking, completed Pomodoro count, and calendar streaks.
- Interactive custom weekly Jetpack Compose Canvas bar chart.
- Subject-wise breakdown (Computer Science, Mathematics, Physics, Languages, etc.) and full session history with deletion support.

#### 7. 🎨 UI/UX & Theming
- 5 custom accent palettes: *Emerald Dragon*, *Cyber Neon*, *Fiery Sunset*, *Obsidian Night*, and *Deep Ocean*.
- True OLED dark mode and clean paper light mode with smooth Compose animations.

---

### 📲 Installation
1. Download **`Timey-v1.1.0.apk`** from the Assets below.
2. Open the downloaded file on your Android device.
3. Allow *Install from unknown sources* if prompted.
4. Open **Timey** and grant Notification and Accessibility permissions (for App Blocker) when prompted.
