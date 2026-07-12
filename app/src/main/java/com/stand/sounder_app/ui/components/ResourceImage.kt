package com.stand.sounder_app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.stand.sounder_app.R

/**
 * 通用资源图片组件 —— 统一处理：
 * - icon 为空时显示首字母占位（primaryContainer 背景）
 * - 加载中显示静态占位（外层 surfaceVariant 背景，性能优于 GlideSubcomposition）
 * - 加载失败显示矢量破损图标占位
 *
 * 使用 GlideImage + placeholder(resourceId)，避免 GlideSubcomposition 的状态重组开销。
 *
 * 调用方通过 [modifier] 指定尺寸（如 Modifier.size(56.dp)），[cornerRadius] 指定圆角。
 */
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun ResourceImage(
    icon: String,
    displayName: String,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 12.dp
) {
    if (icon.isNotEmpty()) {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(cornerRadius))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            GlideImage(
                model = icon,
                contentDescription = displayName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                failure = placeholder(R.drawable.ic_broken_image)
            )
        }
    } else {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(cornerRadius))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_broken_image),
                contentDescription = displayName,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

