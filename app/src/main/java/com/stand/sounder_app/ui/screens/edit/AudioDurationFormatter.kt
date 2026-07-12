package com.stand.sounder_app.ui.screens.edit

/**
 * 将毫秒时长格式化为 "x.x秒" 形式（保留一位小数）。
 */
fun formatAudioDuration(millis: Long): String {
    val seconds = millis / 1000.0
    return String.format("%.1fs", seconds)
}
