package com.stand.sounder_app

import android.app.Application
import android.content.res.Configuration
import android.content.res.Resources
import com.stand.sounder_app.audio.AudioPlayerManager
import com.stand.sounder_app.data.api.RetrofitClient
import com.stand.sounder_app.data.db.AppDatabase
import com.stand.sounder_app.data.download.DownloadManager
import com.stand.sounder_app.data.repository.ResourceRepository
import com.stand.sounder_app.data.repository.SettingsRepository
import com.stand.sounder_app.data.repository.SubmissionRepository
import java.io.File
import java.util.Locale

class MyApp : Application() {

    lateinit var database: AppDatabase
    lateinit var resourceRepository: ResourceRepository
    lateinit var settingsRepository: SettingsRepository
    lateinit var submissionRepository: SubmissionRepository
    lateinit var downloadManager: DownloadManager
    lateinit var audioPlayerManager: AudioPlayerManager

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = AppDatabase.getInstance(this)
        resourceRepository = ResourceRepository(
            resourceDao = database.resourceDao(),
            apiService = RetrofitClient.apiService
        )
        settingsRepository = SettingsRepository(this)
        submissionRepository = SubmissionRepository(
            apiService = RetrofitClient.apiService
        )
        downloadManager = DownloadManager(
            cacheDir = File(filesDir, "audio_cache"),
            repository = resourceRepository
        )
        // 恢复上次未完成的下载记录（.download），使各页面进入时显示「可继续」状态与进度
        downloadManager.restorePendingDownloads(filesDir)
        audioPlayerManager = AudioPlayerManager(this)
        applyLanguage()
    }

    /** 根据设置的语言偏好更新 Context 的 Locale */
    fun applyLanguage() {
        val lang = settingsRepository.language
        val locale = when (lang) {
            SettingsRepository.LANG_ZH -> Locale.SIMPLIFIED_CHINESE
            SettingsRepository.LANG_ZH_TW -> Locale.TRADITIONAL_CHINESE
            SettingsRepository.LANG_EN -> Locale.ENGLISH
            SettingsRepository.LANG_JA -> Locale.JAPANESE
            SettingsRepository.LANG_RU -> Locale.forLanguageTag("ru")
            else -> Resources.getSystem().configuration.locales.get(0)
        }
        Locale.setDefault(locale)
        val config = Configuration(resources.configuration).apply {
            setLocale(locale)
        }
        @Suppress("DEPRECATION")
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    companion object {
        lateinit var instance: MyApp
            private set
    }
}
