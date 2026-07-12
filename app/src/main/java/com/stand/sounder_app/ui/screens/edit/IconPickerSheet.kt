package com.stand.sounder_app.ui.screens.edit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stand.sounder_app.ui.components.ResourceImage
import com.stand.sounder_app.ui.theme.AccentBlue
import com.stand.sounder_app.viewmodel.IconOption

// ===== 图标选择弹窗 =====
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IconPickerSheet(
    currentIcon: String,
    options: List<IconOption>,
    onSelectIcon: (String) -> Unit,
    onPickFromFile: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "选择图标",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (options.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "暂无其他已安装资源的图标",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val columns = 4
                options.chunked(columns).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { option ->
                            val isSelected = currentIcon == option.icon
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) AccentBlue.copy(alpha = 0.15f)
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    )
                                    .clickable { onSelectIcon(option.icon) },
                                contentAlignment = Alignment.Center
                            ) {
                                ResourceImage(
                                    icon = option.icon,
                                    displayName = option.displayName,
                                    modifier = Modifier.fillMaxSize(),
                                    cornerRadius = 8.dp
                                )
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .size(18.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(AccentBlue),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("✓", fontSize = 10.sp, color = Color.White,
                                            fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                        repeat(columns - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ← 从文件选择图标
            PickFromFileButton(
                text = "从文件选择",
                onClick = onPickFromFile
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
