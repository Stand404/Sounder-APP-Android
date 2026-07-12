package com.stand.sounder_app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.stand.sounder_app.R

/**
 * 通用居中状态组件，统一处理空状态 / 加载中 / 加载失败等场景。
 *
 * - [isLoading] 为 true 时显示加载指示器（忽略 [icon]）。
 * - [isError] 为 true 时标题使用错误色。
 * - [onRetry] 不为 null 时显示「重试」按钮（错误态常用）。
 * - [action] 用于其它自定义操作（如「前往商店」）。
 */
@Composable
fun EmptyState(
    icon: @Composable () -> Unit = {
        Icon(
            painter = painterResource(R.drawable.ic_box),
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    },
    title: String,
    subtitle: String = "",
    modifier: Modifier = Modifier,
    action: @Composable (() -> Unit)? = null,
    isLoading: Boolean = false,
    isError: Boolean = false,
    onRetry: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator()
        } else {
            icon()
        }
        if (title.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = if (isError) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        if (subtitle.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
        if (onRetry != null) {
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onRetry) {
                Text(stringResource(R.string.retry))
            }
        } else if (action != null) {
            Spacer(modifier = Modifier.height(24.dp))
            action()
        }
    }
}
