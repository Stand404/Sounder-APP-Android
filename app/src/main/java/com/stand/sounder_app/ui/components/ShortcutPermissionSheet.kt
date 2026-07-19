package com.stand.sounder_app.ui.components

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.stand.sounder_app.R

/**
 * 当「创建桌面快捷方式」权限被厂商拒绝 / 需要询问时，弹出引导弹窗，
 * 引导用户前往系统设置开启权限；也可选择「仍然尝试」直接发起添加。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShortcutPermissionSheet(
    onDismiss: () -> Unit,
    onGoToSettings: () -> Unit,
    onTryAnyway: () -> Unit
) {
    val guide = when (val m = Build.MANUFACTURER.lowercase()) {
        "xiaomi" -> stringResource(R.string.shortcut_guide_xiaomi)
        "huawei" -> stringResource(R.string.shortcut_guide_huawei)
        "oppo" -> stringResource(R.string.shortcut_guide_oppo)
        "vivo" -> stringResource(R.string.shortcut_guide_vivo)
        else -> stringResource(R.string.shortcut_guide_other)
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = stringResource(R.string.shortcut_permission_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = guide,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp)
            )
            Text(
                text = stringResource(R.string.shortcut_steps_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = stringResource(R.string.shortcut_tutorial_steps),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
            ) {
                TextButton(onClick = onTryAnyway) {
                    Text(stringResource(R.string.shortcut_try_anyway))
                }
                Button(onClick = onGoToSettings) {
                    Text(stringResource(R.string.go_to_settings))
                }
            }
        }
    }
}
