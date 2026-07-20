## 中文 | [English](EN_README.md)

<div align="center">
  <img src="app/src/main/res/drawable/ico.png" width="64" alt="发声APP图标"/>
  <h1>发声APP · 造化版（Android）</h1>
  <p>一款基于 Jetpack Compose 构建的原生 Android 应用</p>
</div>

## ✨ 功能介绍

### <img src="app/src/main/res/drawable/sounder.png" width="28" style="vertical-align: middle;" alt=""/> 发声APP —— 初版
一款点击后只会播放声音的原生 Android 解压小软件。第一次启动时选择音频文件，之后每次点击图标直接播放声音，不弹出界面。  
初版仓库：[https://github.com/Stand404/Sounder](https://github.com/Stand404/Sounder)

### <img src="app/src/main/res/drawable/sounder.png" width="28" style="vertical-align: middle;" alt=""/> 发声APP —— 独立版
在初版基础上打造的一系列独立 Android APP，每个 APP 拥有独立的图标和默认音频，点击即播，轻松解压。  
可以在官网获取：https://stand.homes/apps

### <img src="app/src/main/res/drawable/ico.png" width="28" style="vertical-align: middle;" alt=""/> 发声APP · 造化版（本程序）
在独立版概念基础上全面扩展的完整功能版，支持浏览、搜索、下载、创建、编辑各种声音资源包，播放各类音频资源。  
当前项目为 **造化版的 Android 端**，与 [造化版桌面端](https://github.com/stand404/Sounder-APP-Desktop)（支持 Windows、macOS、Linux）功能对应。

### 核心功能
- **在线商店** — 浏览、搜索和下载音频资源包
- **音频资源管理** — 浏览、播放和管理本地音频文件
- **编辑与创建** — 创建和编辑自定义资源包
- **多模式播放** — 支持叠加播放、替换播放、循环播放
- **任务管理** — 桌面播放任务列表展示和控制
- **快捷方式** — 一键创建桌面快捷方式，点击即播
- **投稿板块** — 提交投稿和查看投稿列表
- **多语言支持** — 支持简体中文、繁体中文、English、日本語、Русский，即时切换

---

## 🖥️ 系统要求

| 项目 | 要求 |
|------|------|
| **Android** | Android 8.0 (API 26) 及以上 |
| **存储** | 约 20MB 可用空间（视资源包大小而定） |

---

## 📦 下载与安装

### 获取 APK

前往以下地址下载最新版本 APK：**https://stand.homes/apps/e95a1dab-2f24-4557-ba9d-98e82861705d**  
或通过 GitHub Releases 页面：**https://github.com/stand404/Sounder/releases**

### 安装说明

下载 APK 后，在手机上打开即可安装。如果提示「未知来源应用」，请在设置中允许安装未知来源应用。

---

## 🔧 自编译

### 前置要求
- [Android Studio](https://developer.android.com/studio) (推荐最新稳定版)
- **JDK 21** 或更高版本
- **Android SDK 36**

### 开发环境参考

| 工具 | 版本 |
|------|------|
| **Android Studio** | 25.1 (或更高) |
| **Java** | 21 |
| **Android SDK** | 36 |
| **Kotlin** | 2.0.21 |
| **AGP** | 8.13.2 |

### 编译运行

1. 克隆项目并导入 Android Studio：

```bash
git clone https://github.com/stand404/Sounder.git
```

2. 在项目根目录创建 `local.properties` 文件，填写签名信息（用于 Release 构建）：

```properties
RELEASE_STORE_FILE=/path/to/your/keystore.jks
RELEASE_STORE_PASSWORD=your_store_password
RELEASE_KEY_ALIAS=your_key_alias
RELEASE_KEY_PASSWORD=your_key_password
```

> **注意**：Debug 构建同样使用 Release 签名配置。如需跳过签名，请修改 `app/build.gradle` 中 `debug` 的 `signingConfig`。

3. 使用 Android Studio 打开项目，点击 **Run** 或执行：

```bash
# Windows
gradlew assembleDebug

# macOS / Linux
./gradlew assembleDebug
```

### 发布构建（Release）

```bash
# Windows
gradlew assembleRelease

# macOS / Linux
./gradlew assembleRelease
```

产物位于 `app/build/outputs/apk/release/` 目录。

---

## 📁 项目结构

```
Sounder-APP/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/stand/sounder_app/
│   │   │   │   ├── MainActivity.kt              # 主 Activity
│   │   │   │   ├── MyApp.kt                     # Application 类
│   │   │   │   ├── audio/
│   │   │   │   │   └── AudioPlayerManager.kt    # 音频播放管理器
│   │   │   │   ├── shortcut/
│   │   │   │   │   ├── ShortcutPlayActivity.kt  # 快捷播放 Activity
│   │   │   │   │   └── ShortcutPlayReceiver.kt  # 快捷播放广播
│   │   │   │   ├── data/
│   │   │   │   │   ├── api/                     # Retrofit API 接口
│   │   │   │   │   ├── db/                      # Room 数据库 (DAO, Entity)
│   │   │   │   │   ├── download/                # 下载管理器
│   │   │   │   │   ├── model/                   # 数据模型
│   │   │   │   │   └── repository/              # 数据仓库
│   │   │   │   ├── viewmodel/                   # ViewModel
│   │   │   │   ├── util/                        # 工具类
│   │   │   │   └── ui/
│   │   │   │       ├── navigation/              # 导航图与路由
│   │   │   │       ├── theme/                   # Material3 主题
│   │   │   │       ├── components/              # 通用组件
│   │   │   │       └── screens/                 # 页面
│   │   │   │           ├── shop/                # 商店
│   │   │   │           ├── detail/              # 详情
│   │   │   │           ├── personal/            # 个人资源
│   │   │   │           ├── search/              # 搜索
│   │   │   │           ├── edit/                # 编辑
│   │   │   │           ├── settings/            # 设置
│   │   │   │           ├── submissions/         # 提交管理
│   │   │   │           └── tasks/               # 任务管理
│   │   │   ├── res/
│   │   │   │   ├── drawable/                    # 图标与图片资源
│   │   │   │   ├── values/                      # 简体中文 (默认)
│   │   │   │   ├── values-en/                   # English
│   │   │   │   ├── values-ja/                   # 日本語
│   │   │   │   ├── values-ru/                   # Русский
│   │   │   │   └── values-zh-rTW/               # 繁體中文
│   │   │   └── AndroidManifest.xml
│   │   └── ...
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── gradle/
│   └── libs.versions.toml                       # 版本目录
├── build.gradle                                 # 根构建文件
├── settings.gradle                              # 项目设置
├── gradle.properties                            # Gradle 配置
├── gradlew / gradlew.bat                        # Gradle Wrapper
├── LICENSE
├── EN_README.md
└── README.md
```

---

## 📄 协议

MIT © Stand404
