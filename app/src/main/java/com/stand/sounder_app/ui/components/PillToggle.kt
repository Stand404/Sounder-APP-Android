package com.stand.sounder_app.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class PillOption<T>(
    val value: T,
    val label: String
)

@Composable
fun <T> PillToggle(
    options: List<PillOption<T>>,
    selectedValue: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    indicatorColors: List<Color>? = null
) {
    val selectedIndex = options.indexOfFirst { it.value == selectedValue }.coerceAtLeast(0)
    val indicatorColor = indicatorColors?.getOrNull(selectedIndex) ?: MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .widthIn(max = 180.dp)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        val itemWeight = 1f / options.size

        // 滑动指示器（叠加层）
        val totalWidth = remember { mutableIntStateOf(0) }
        val density = LocalDensity.current
        val indicatorOffset by animateDpAsState(
            targetValue = if (totalWidth.intValue > 0) {
                // totalWidth.value 为像素值，先转为 dp 再计算偏移
                val totalDp = with(density) { totalWidth.intValue.toDp() }
                (totalDp - 8.dp) * itemWeight * selectedIndex
            } else 0.dp,
            animationSpec = tween(300),
            label = "pillOffset"
        )

        Box(modifier = Modifier.matchParentSize().padding(4.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(itemWeight)
            .offset(x = indicatorOffset)
            .clip(RoundedCornerShape(45))
            .background(indicatorColor)
            )
        }

        // 选项行
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { totalWidth.intValue = it.width }
                .padding(vertical = 2.dp, horizontal = 3.dp)
        ) {
            options.forEach { option ->
                val isSelected = option.value == selectedValue
                Box(
                    modifier = Modifier
                        .weight(itemWeight)
                        .clip(RoundedCornerShape(50))
                        .clickable { onSelect(option.value) }
                        .padding(vertical = 3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = option.label,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
