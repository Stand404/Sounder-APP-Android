package com.stand.sounder_app.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.stand.sounder_app.MyApp
import com.stand.sounder_app.data.repository.SettingsRepository
import com.stand.sounder_app.util.formatByteSize
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = MyApp.instance.settingsRepository

    val authorName: String = SettingsRepository.AUTHOR_NAME
    val websiteUrl: String = SettingsRepository.WEBSITE_URL
    val packageUrl: String = SettingsRepository.PACKAGE_URL
    val audioFilesUrl: String = SettingsRepository.AUDIO_FILES_URL

    private val _cacheSizeText = MutableStateFlow("计算中...")
    val cacheSizeText: StateFlow<String> = _cacheSizeText.asStateFlow()

    /** 直接引用仓库的共享主题流，确保 MainActivity 能感知变化并重新套用主题 */
    val themeMode: StateFlow<String> = settingsRepository.themeModeFlow

    /** 语言设置 */
    val language: StateFlow<String> = settingsRepository.languageFlow

    init {
        refreshCacheSize()
    }

    fun setThemeMode(mode: String) {
        settingsRepository.themeMode = mode
    }

    fun setLanguage(lang: String) {
        settingsRepository.language = lang
        // 同步更新 Application 级别的资源，确保 MyApp.instance.getString() 返回正确语言
        MyApp.instance.applyLanguage()
    }

    fun refreshCacheSize() {
        viewModelScope.launch {
            val bytes = settingsRepository.getCacheSize()
            _cacheSizeText.value = formatByteSize(bytes)
        }
    }

    fun clearCache(onDone: () -> Unit = {}) {
        viewModelScope.launch {
            settingsRepository.clearCache()
            refreshCacheSize()
            onDone()
        }
    }

    /** 通过系统浏览器打开外部链接 */
    fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        getApplication<Application>().startActivity(intent)
    }
}
