package com.stand.sounder_app.shortcut

import android.app.Activity
import android.os.Bundle
import android.util.Log
import com.stand.sounder_app.MyApp
import com.stand.sounder_app.R
import com.stand.sounder_app.data.model.OrderMode
import com.stand.sounder_app.data.model.PlayMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

/**
 * 透明 Activity：承接桌面快捷方式的点击。
 * 固定快捷方式指向 BroadcastReceiver 时，部分启动器会解析失败（提示"未安装应用"），
 * 因此改为指向本透明 Activity，点击后直接启动播放并自行关闭，不展示任何界面。
 * 循环模式下若该资源已在播放，第二次点击为「关闭」（参考 BackgroundPlaybackManager.PlayResourceAsync）。
 */
class ShortcutPlayActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val resourceId = intent?.getStringExtra(ShortcutPlayReceiver.EXTRA_RESOURCE_ID)
        if (resourceId != null) {
            val app = application as? MyApp
            if (app != null) {
                val resource = runBlocking(Dispatchers.IO) {
                    app.resourceRepository.getLocalResourceById(resourceId)
                }
                if (resource == null) {
                    android.widget.Toast.makeText(
                        this,
                        getString(R.string.shortcut_resource_deleted),
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                } else if (resource.audioList.isEmpty()) {
                    android.widget.Toast.makeText(
                        this,
                        getString(R.string.shortcut_no_audio),
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                } else {
                    val audioPlayer = app.audioPlayerManager
                    // 循环模式下，若该资源已在播放，第二次点击切换为停止
                    if (resource.playMode == PlayMode.LOOP && audioPlayer.isResourcePlaying(resourceId)) {
                        Log.i("ShortcutPlayActivity", "循环模式已在播放，切换为停止: id=$resourceId")
                        audioPlayer.stopResource(resourceId)
                    } else {
                        audioPlayer.setPlaySettings(resource.playMode, resource.orderMode, resource.loopMode)

                        // 根据顺序/随机模式选择首次播放的音频索引（参照 C# SelectAudio）
                        val tempAudioList = resource.audioList
                        val startIndex = if (resource.orderMode == OrderMode.RANDOM) {
                            tempAudioList.indices.random()
                        } else {
                            resource.currentAudioIndex.coerceIn(0, tempAudioList.lastIndex)
                        }
                        // 顺序模式下，播放后前进一位并持久化（下次快捷方式点击继续衔接）
                        if (resource.orderMode == OrderMode.ORDER) {
                            val nextIdx = (startIndex + 1) % tempAudioList.size
                            runBlocking(Dispatchers.IO) {
                                app.resourceRepository.updateCurrentAudioIndex(resource.id, nextIdx)
                            }
                        }
                        audioPlayer.play(
                            audioList = resource.audioList,
                            resourceId = resource.id,
                            displayName = resource.name,
                            icon = resource.icon,
                            startIndex = startIndex,
                            onLoopIndexChanged = { idx ->
                                runBlocking(Dispatchers.IO) {
                                    app.resourceRepository.updateCurrentAudioIndex(resource.id, idx)
                                }
                            }
                        )
                    }
                }
            }
        }

        // 无需界面，直接结束
        finish()
    }

    companion object {
        const val ACTION_PLAY_SHORTCUT = "com.stand.sounder_app.action.PLAY_SHORTCUT"
    }
}
