package com.stand.sounder_app.ui.screens.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.stand.sounder_app.ui.components.shimmerEffect

@Composable
fun DetailSkeleton(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)) {
        // ===== Header 骨架 =====
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                // 图标
                Box(
                    modifier = Modifier
                        .width(70.dp)
                        .height(70.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .shimmerEffect()
                )
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    // 标题
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .height(18.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .shimmerEffect()
                    )
                    Spacer(Modifier.height(8.dp))
                    // 描述
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height(13.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .shimmerEffect()
                    )
                    Spacer(Modifier.height(8.dp))
                    // 大小/日期 pill
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            modifier = Modifier
                                .width(72.dp)
                                .height(20.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .shimmerEffect()
                        )
                        Box(
                            modifier = Modifier
                                .width(96.dp)
                                .height(20.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .shimmerEffect()
                        )
                    }
                }
            }
        }

        // ===== 音频列表区域骨架 =====
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp)
        ) {
            // "在线试听" 行 + PillToggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.25f)
                        .height(16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmerEffect()
                )
                Box(
                    modifier = Modifier
                        .width(100.dp)
                        .height(28.dp)
                        .clip(RoundedCornerShape(50))
                        .shimmerEffect()
                )
            }

            Spacer(Modifier.height(16.dp))

            // 音频列表项 x6
            repeat(6) {
                AudioSkeletonItem()
                if (it < 5) {
                    Spacer(Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(0.5.dp)
                            .padding(start = 4.dp)
                            .shimmerEffect()
                    )
                    Spacer(Modifier.height(2.dp))
                }
            }
        }
    }
}

@Composable
private fun AudioSkeletonItem() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            // 音频名
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(15.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .shimmerEffect()
            )
            Spacer(Modifier.height(6.dp))
            // 时长
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.15f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .shimmerEffect()
            )
        }
        Spacer(Modifier.width(12.dp))
        // 右侧状态图标占位
        Box(
            modifier = Modifier
                .width(24.dp)
                .height(24.dp)
                .shimmerEffect()
        )
    }
}
