package com.stand.sounder_app.util

import android.content.Context
import android.net.Uri
import com.stand.sounder_app.MyApp
import com.stand.sounder_app.data.model.AudioItem
import com.stand.sounder_app.data.model.Resource
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import org.json.JSONObject

/** 导入结果 */
data class ImportResult(
    val displayName: String,
    val description: String,
    val audioFiles: List<AudioItem>,
    val iconPath: String,
    val publishDate: String
)

object ResourcePackageUtils {

    /** 构建 manifest.json，与 C# ExportManifest 格式一致 */
    fun buildManifest(resource: Resource): String {
        val audioItems = resource.audioList.joinToString(",\n") { audio ->
            """      { "name": ${jsonStr(audio.name)}, "durationMs": ${audio.duration} }"""
        }
        val iconFile = File(resource.icon)
        val iconFileName = "icon.${iconFile.extension}"
        val audioFileNames = resource.audioList.mapIndexed { i, _ ->
            val audioFile = File(resource.audioList[i].src)
            "audio_$i.${audioFile.extension}"
        }
        return buildString {
            appendLine("{")
            appendLine("  \"version\": 1,")
            appendLine("  \"exportDate\": \"${java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US).format(java.util.Date())}\",")
            appendLine("  \"resource\": {")
            appendLine("    \"displayName\": ${jsonStr(resource.displayName)},")
            appendLine("    \"description\": ${jsonStr(resource.description)},")
            appendLine("    \"audioItems\": [")
            appendLine(audioItems)
            appendLine("    ]")
            appendLine("  },")
            appendLine("  \"files\": {")
            appendLine("    \"icon\": ${jsonStr(iconFileName)},")
            appendLine("    \"audios\": [")
            audioFileNames.forEachIndexed { i, name ->
                append("      ${jsonStr(name)}")
                if (i < audioFileNames.lastIndex) appendLine(",") else appendLine()
            }
            appendLine("    ]")
            appendLine("  }")
            appendLine("}")
        }
    }

    /** 导出资源为 zip 文件（与 C# 导出格式完全一致） */
    fun createExportZip(
        resource: Resource,
        destFile: File
    ): Boolean {
        return runCatching {
            ZipOutputStream(BufferedOutputStream(FileOutputStream(destFile))).use { zos ->
                // manifest.json
                val manifest = buildManifest(resource)
                zos.putNextEntry(ZipEntry("manifest.json"))
                zos.write(manifest.toByteArray(Charsets.UTF_8))
                zos.closeEntry()

                // 图标（zip 根目录）
                val iconFile = File(resource.icon)
                if (iconFile.exists()) {
                    zos.putNextEntry(ZipEntry("icon.${iconFile.extension}"))
                    BufferedInputStream(FileInputStream(iconFile)).use { bis -> bis.copyTo(zos) }
                    zos.closeEntry()
                }

                // 音频（zip audios/ 目录）
                resource.audioList.forEachIndexed { i, audio ->
                    val audioFile = File(audio.src)
                    if (audioFile.exists()) {
                        val entryName = "audios/audio_$i.${audioFile.extension}"
                        zos.putNextEntry(ZipEntry(entryName))
                        BufferedInputStream(FileInputStream(audioFile)).use { bis -> bis.copyTo(zos) }
                        zos.closeEntry()
                    }
                }
            }
        }.isSuccess
    }

    /**
     * 将 zip 解压到 installDir 并解析 manifest，返回导入数据。
     * 返回的 ImportResult 中 iconPath 和 audioFiles[].src 直接指向 installDir 中的文件。
     */
    fun parseImportZip(context: Context, uri: Uri, installDir: File): ImportResult {
        installDir.mkdirs()

        // 解压到 installDir
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            ZipInputStream(BufferedInputStream(inputStream)).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val targetFile = File(installDir, entry.name.replace('/', File.separatorChar))
                        targetFile.parentFile?.mkdirs()
                        FileOutputStream(targetFile).use { fos -> zis.copyTo(fos) }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
        }

        // 查找内容根目录（兼容 zip 根目录或嵌套一级目录）
        val contentRoot = if (File(installDir, "manifest.json").exists()) installDir
        else installDir.listFiles()?.firstOrNull { dir ->
            dir.isDirectory && File(dir, "manifest.json").exists()
        } ?: throw RuntimeException("找不到 manifest.json")

        // 如果存在嵌套目录，将内容移动到 installDir 根目录
        if (contentRoot != installDir) {
            contentRoot.listFiles()?.forEach { file ->
                file.renameTo(File(installDir, file.name))
            }
            contentRoot.delete()
        }

        // 解析 manifest.json
        val manifestFile = File(installDir, "manifest.json")
        val manifestJson = JSONObject(manifestFile.readText(Charsets.UTF_8))
        val resourceJson = manifestJson.getJSONObject("resource")
        val displayName = resourceJson.getString("displayName")
        val description = resourceJson.optString("description", "")
        val rawExportDate = manifestJson.optString("exportDate", "")
        val publishDate = if (rawExportDate.length >= 10) rawExportDate.substring(0, 10) else ""

        // 解析 audioItems
        val audioArr = resourceJson.getJSONArray("audioItems")
        val audioFiles = mutableListOf<AudioItem>()
        for (i in 0 until audioArr.length()) {
            val item = audioArr.getJSONObject(i)
            audioFiles.add(AudioItem(
                id = "tmp_audio_$i",
                name = item.getString("name"),
                src = "",
                duration = item.optLong("durationMs", 0L)
            ))
        }

        // 解析 files 段
        val filesJson = manifestJson.getJSONObject("files")

        // 导入图标
        var iconPath = ""
        val iconName = filesJson.optString("icon", "")
        if (iconName.isNotBlank()) {
            val iconSrc = File(installDir, iconName)
            if (iconSrc.exists()) {
                iconPath = iconSrc.absolutePath
            }
        }

        // 导入音频
        val audiosArr = filesJson.getJSONArray("audios")
        for (i in 0 until audiosArr.length()) {
            val audioEntryName = audiosArr.getString(i)
            val audioSrc = File(installDir, "audios/$audioEntryName")
            if (audioSrc.exists() && i < audioFiles.size) {
                audioFiles[i] = audioFiles[i].copy(src = audioSrc.absolutePath)
            }
        }

        return ImportResult(
            displayName = displayName,
            description = description,
            audioFiles = audioFiles,
            iconPath = iconPath,
            publishDate = publishDate
        )
    }

    // ===== 通用工具 =====

    /** 获取资源安装目录 */
    fun resourceDir(resourceId: String): File =
        File(MyApp.instance.filesDir, "audio/$resourceId")

    /** 复制目录 */
    fun copyDirectory(src: File, dest: File) {
        if (!src.exists() || !src.isDirectory) return
        dest.mkdirs()
        src.copyRecursively(dest, overwrite = true)
    }

    /** 清理文件名中的非法字符 */
    fun sanitizeFileName(name: String): String {
        val invalid = listOf('/', '\\', ':', '*', '?', '"', '<', '>', '|')
        val sanitized = name.map { if (it in invalid) '_' else it }.joinToString("")
        return sanitized.ifBlank { "resource" }
    }

    /** JSON 字符串转义 */
    fun jsonStr(value: String): String =
        "\"${value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")}\""

}
