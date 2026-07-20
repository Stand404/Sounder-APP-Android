## English | [中文](README.md)

<div align="center">
  <img src="app/src/main/res/drawable/ico.png" width="64" alt="Sounder-APP Icon"/>
  <h1>Sounder APP · Enhanced Edition (Android)</h1>
  <p>A native Android app built with Jetpack Compose</p>
</div>

## ✨ Features

### <img src="app/src/main/res/drawable/sounder.png" width="28" style="vertical-align: middle;" alt=""/> Sounder-APP — Original Edition
A native Android stress relief app that just plays a sound when tapped. Pick an audio file on first launch, then every subsequent tap plays the sound directly — no UI, no fuss.  
Original repository: [https://github.com/Stand404/Sounder](https://github.com/Stand404/Sounder)

### <img src="app/src/main/res/drawable/sounder.png" width="28" style="vertical-align: middle;" alt=""/> Sounder-APP — Standalone Edition (Android Only)
A series of standalone Android apps built on the original version, each with its own icon and default sound — tap to play, instant stress relief. Available on the official website: https://stand.homes/apps

### <img src="app/src/main/res/drawable/ico.png" width="28" style="vertical-align: middle;" alt=""/> Sounder-APP · Enhanced Edition (This App)
A full-featured expansion built on the standalone edition concept. Browse, search, download, create, and edit sound resource packs, play various audio resources.  
This project is the **Android version** of the Enhanced Edition, corresponding to the [Desktop Enhanced Edition](https://github.com/stand404/Sounder-APP-Desktop) (supports Windows, macOS, Linux).

### Core Features
- **Online Store** — Browse, search, and download audio resource packs
- **Audio Resource Management** — Browse, play and manage local audio files
- **Edit & Create** — Create and edit custom resource packs
- **Multi-Mode Playback** — Overlay, replace, and loop playback modes
- **Task Management** — Desktop playback task list display and control
- **Shortcuts** — One-tap shortcut creation on the home screen
- **Submission Portal** — Submit and view your submissions
- **Multi-Language Support** — Switch instantly between English, Simplified Chinese, Traditional Chinese, 日本語, Русский

---

## 🖥️ System Requirements

| Item | Requirement |
|------|-------------|
| **Android** | Android 8.0 (API 26) and above |
| **Storage** | ~20MB free space (varies by resource pack size) |

---

## 📦 Download & Installation

### Get the APK

Download the latest APK from: **https://stand.homes/apps/e95a1dab-2f24-4557-ba9d-98e82861705d**  
Or via GitHub Releases: **https://github.com/stand404/Sounder/releases**

### Installation

Open the downloaded APK on your phone to install. If prompted about "unknown sources", allow installation from unknown sources in your system settings.

---

## 🔧 Build from Source

### Prerequisites
- [Android Studio](https://developer.android.com/studio) (latest stable recommended)
- **JDK 21** or higher
- **Android SDK 36**

### Development Environment Reference

| Tool | Version |
|------|---------|
| **Android Studio** | 25.1 (or higher) |
| **Java** | 21 |
| **Android SDK** | 36 |
| **Kotlin** | 2.0.21 |
| **AGP** | 8.13.2 |

### Build & Run

1. Clone the project and open in Android Studio:

```bash
git clone https://github.com/stand404/Sounder.git
```

2. Create a `local.properties` file in the project root with signing info (required for Release builds):

```properties
RELEASE_STORE_FILE=/path/to/your/keystore.jks
RELEASE_STORE_PASSWORD=your_store_password
RELEASE_KEY_ALIAS=your_key_alias
RELEASE_KEY_PASSWORD=your_key_password
```

> **Note**: Debug builds also use the Release signing config. If you want to skip signing, modify `signingConfig` in the `debug` block of `app/build.gradle`.

3. Open the project in Android Studio and click **Run**, or execute:

```bash
# Windows
gradlew assembleDebug

# macOS / Linux
./gradlew assembleDebug
```

### Release Build

```bash
# Windows
gradlew assembleRelease

# macOS / Linux
./gradlew assembleRelease
```

Output APK will be in `app/build/outputs/apk/release/`.

---

## 📁 Project Structure

```
Sounder-APP/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/stand/sounder_app/
│   │   │   │   ├── MainActivity.kt              # Main Activity
│   │   │   │   ├── MyApp.kt                     # Application class
│   │   │   │   ├── audio/
│   │   │   │   │   └── AudioPlayerManager.kt    # Audio player manager
│   │   │   │   ├── shortcut/
│   │   │   │   │   ├── ShortcutPlayActivity.kt  # Shortcut play Activity
│   │   │   │   │   └── ShortcutPlayReceiver.kt  # Shortcut play receiver
│   │   │   │   ├── data/
│   │   │   │   │   ├── api/                     # Retrofit API interfaces
│   │   │   │   │   ├── db/                      # Room database (DAO, Entity)
│   │   │   │   │   ├── download/                # Download manager
│   │   │   │   │   ├── model/                   # Data models
│   │   │   │   │   └── repository/              # Data repositories
│   │   │   │   ├── viewmodel/                   # ViewModels
│   │   │   │   ├── util/                        # Utilities
│   │   │   │   └── ui/
│   │   │   │       ├── navigation/              # Nav graph & routes
│   │   │   │       ├── theme/                   # Material3 theme
│   │   │   │       ├── components/              # Shared components
│   │   │   │       └── screens/                 # Screens
│   │   │   │           ├── shop/                # Online store
│   │   │   │           ├── detail/              # Resource details
│   │   │   │           ├── personal/            # Personal resources
│   │   │   │           ├── search/              # Search
│   │   │   │           ├── edit/                # Resource pack editor
│   │   │   │           ├── settings/            # Settings
│   │   │   │           ├── submissions/         # Submission management
│   │   │   │           └── tasks/               # Download task manager
│   │   │   ├── res/
│   │   │   │   ├── drawable/                    # Icons & images
│   │   │   │   ├── values/                      # Simplified Chinese (default)
│   │   │   │   ├── values-en/                   # English
│   │   │   │   ├── values-ja/                   # 日本語
│   │   │   │   ├── values-ru/                   # Русский
│   │   │   │   └── values-zh-rTW/               # Traditional Chinese
│   │   │   └── AndroidManifest.xml
│   │   └── ...
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── gradle/
│   └── libs.versions.toml                       # Version catalog
├── build.gradle                                 # Root build file
├── settings.gradle                              # Project settings
├── gradle.properties                            # Gradle config
├── gradlew / gradlew.bat                        # Gradle Wrapper
├── LICENSE
├── EN_README.md
└── README.md
```

---

## 📄 License

MIT © Stand404
