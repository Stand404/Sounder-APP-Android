package com.stand.sounder_app.util

/** 将毫秒转换为可读时长格式 */
fun formatDuration(millis: Long): String {
    if (millis <= 0) return "0.0s"
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return if (minutes > 0) "$minutes:${seconds.toString().padStart(2, '0')}"
    else "${millis / 1000}.${(millis % 1000) / 100}s"
}

/** 将字节数转换为可读大小格式 */
fun formatByteSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "%.1f MB".format(bytes.toDouble() / (1024 * 1024))
        else -> "%.2f GB".format(bytes.toDouble() / (1024 * 1024 * 1024))
    }
}
