package com.stand.sounder_app.shortcut

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.stand.sounder_app.MyApp
import com.stand.sounder_app.data.model.OrderMode
import com.stand.sounder_app.data.model.PlayMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

/**
 * 桌面快捷方式点击接收器：直接启动该资源的播放任务，而不打开 App 界面。
 * 对应 C# 端 sounder://play?resourceId= 直接播放的行为（参考 BackgroundPlaybackManager.PlayResourceAsync）：
 * 循环模式下若该资源已在播放，则第二次点击为「关闭」（toggle 关闭），而非再起一个循环。
 */
class ShortcutPlayReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_PLAY_SHORTCUT) return
        val resourceId = intent.getStringExtra(EXTRA_RESOURCE_ID) ?: return

        val app = context.applicationContext as? MyApp ?: return
        // getLocalResourceById 为挂起函数，onReceive 内用 runBlocking 同步读取（仅一次性、开销小）
        val resource = runBlocking(Dispatchers.IO) {
            app.resourceRepository.getLocalResourceById(resourceId)
        }
        if (resource == null) {
            android.widget.Toast.makeText(
                context,
                "资源不存在，可能已被删除",
                android.widget.Toast.LENGTH_SHORT
            ).show()
            return
        }

        val audioList = resource.audioList
        if (audioList.isEmpty()) {
            android.widget.Toast.makeText(
                context,
                "该资源没有可播放的音频文件",
                android.widget.Toast.LENGTH_SHORT
            ).show()
            return
        }

        val audioPlayer = app.audioPlayerManager

        // 循环模式下，若该资源已在播放，第二次点击切换为停止（参考 BackgroundPlaybackManager 模式相同+Loop 分支）
        if (resource.playMode == PlayMode.LOOP && audioPlayer.isResourcePlaying(resourceId)) {
            Log.i("ShortcutPlayReceiver", "循环模式已在播放，切换为停止: id=$resourceId")
            audioPlayer.stopResource(resourceId)
            return
        }

        audioPlayer.setPlaySettings(resource.playMode, resource.orderMode, resource.loopMode)

        // 根据顺序/随机模式选择首次播放的音频索引（参照 C# SelectAudio）
        val startIndex = if (resource.orderMode == OrderMode.RANDOM) {
            audioList.indices.random()
        } else {
            resource.currentAudioIndex.coerceIn(0, audioList.lastIndex)
        }
        // 顺序模式下，播放后前进一位并持久化（下次快捷方式点击继续衔接）
        if (resource.orderMode == OrderMode.ORDER) {
            val nextIdx = (startIndex + 1) % audioList.size
            runBlocking(Dispatchers.IO) {
                app.resourceRepository.updateCurrentAudioIndex(resource.id, nextIdx)
            }
        }
        audioPlayer.play(
            audioList = audioList,
            resourceId = resource.id,
            displayName = resource.displayName,
            shortName = resource.name,
            icon = resource.icon,
            startIndex = startIndex,
            onLoopIndexChanged = { idx ->
                runBlocking(Dispatchers.IO) {
                    app.resourceRepository.updateCurrentAudioIndex(resource.id, idx)
                }
            }
        )
    }

    companion object {
        const val ACTION_PLAY_SHORTCUT = "com.stand.sounder_app.action.PLAY_SHORTCUT"
        const val EXTRA_RESOURCE_ID = "resource_id"
    }
}
