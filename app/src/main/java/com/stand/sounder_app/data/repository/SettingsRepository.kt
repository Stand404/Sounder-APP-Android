package com.stand.sounder_app.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import com.bumptech.glide.Glide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

class SettingsRepository(context: Context) {

    private val appContext = context.applicationContext
    private val prefs: SharedPreferences =
        context.getSharedPreferences("sounder_settings", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_LANGUAGE = "language"

        const val THEME_AUTO = "Auto"
        const val THEME_LIGHT = "Light"
        const val THEME_DARK = "Dark"

        const val LANG_SYSTEM = "System"
        const val LANG_ZH = "zh"
        const val LANG_ZH_TW = "zh-rTW"
        const val LANG_EN = "en"
        const val LANG_JA = "ja"
        const val LANG_RU = "ru"

        const val AUTHOR_NAME = "Stand404"
        const val WEBSITE_URL = "https://stand.homes"
        const val PACKAGE_URL =
            "https://stand.homes/apps/a4afacef-96c4-40dc-a5b4-be1e55d73df1"
        const val AUDIO_FILES_URL =
            "https://stand.homes/apps/2b6f4e18-bafe-46bf-b162-056fa277c82f"
    }

    /** 主题模式：Auto / Light / Dark（与后端/桌面端一致） */
    var themeMode: String
        get() = prefs.getString(KEY_THEME_MODE, THEME_AUTO) ?: THEME_AUTO
        set(value) {
            prefs.edit().putString(KEY_THEME_MODE, value).apply()
            _themeModeFlow.value = value
        }

    /** 共享主题流：任意 ViewModel 都引用同一个实例，确保修改后全局生效 */
    private val _themeModeFlow = MutableStateFlow(themeMode)
    val themeModeFlow: StateFlow<String> = _themeModeFlow.asStateFlow()

    /** 语言偏好 */
    var language: String
        get() = prefs.getString(KEY_LANGUAGE, LANG_SYSTEM) ?: LANG_SYSTEM
        set(value) {
            prefs.edit().putString(KEY_LANGUAGE, value).apply()
            _languageFlow.value = value
        }

    private val _languageFlow = MutableStateFlow(language)
    val languageFlow: StateFlow<String> = _languageFlow.asStateFlow()

    /** 根据设置判断当前是否为深色模式 */
    @Composable
    fun isDarkTheme(): Boolean {
        return when (themeMode) {
            THEME_LIGHT -> false
            THEME_DARK -> true
            else -> isSystemInDarkTheme()
        }
    }

    /** 获取缓存大小（字节）：图片缓存(Glide 磁盘缓存) + 试听音频缓存 */
    suspend fun getCacheSize(): Long = withContext(Dispatchers.IO) {
        val glideCache = File(appContext.cacheDir, "glide")
        val audioCache = File(appContext.filesDir, "audio_cache")
        dirSize(glideCache) + dirSize(audioCache)
    }

    /** 清理缓存：图片缓存 + 试听音频缓存 */
    suspend fun clearCache(): Unit = withContext(Dispatchers.IO) {
        // 图片缓存（Glide 磁盘缓存）
        runCatching { Glide.get(appContext).clearDiskCache() }
        // 试听音频缓存
        val audioCache = File(appContext.filesDir, "audio_cache")
        audioCache.listFiles()?.forEach { runCatching { it.delete() } }
    }

    private fun dirSize(dir: File): Long {
        if (!dir.exists() || !dir.isDirectory) return 0L
        return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }
}
