package com.stand.sounder_app.ui.screens.edit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.stand.sounder_app.data.model.PickerAudioItem
import com.stand.sounder_app.ui.components.SearchBox
import com.stand.sounder_app.ui.theme.AccentBlue

// ===== 音频选择弹窗 =====
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioPickerSheet(
    searchText: String,
    items: List<PickerAudioItem>,
    selectedIds: Set<String>,
    onSearchChange: (String) -> Unit,
    onToggleItem: (String) -> Unit,
    onPickFromFile: () -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val filteredItems = remember(items, searchText) {
        if (searchText.isBlank()) items
        else items.filter {
            it.name.contains(searchText, ignoreCase = true) ||
            it.sourceName.contains(searchText, ignoreCase = true)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 标题 + 确认按钮
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Text(
                    text = "选择音频",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = onConfirm,
                    enabled = selectedIds.isNotEmpty(),
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentBlue,
                        contentColor = Color.White,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                            .copy(alpha = 0.5f),
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    contentPadding = PaddingValues(horizontal = 20.dp)
                ) {
                    Text("添加", style = MaterialTheme.typography.bodyMedium)
                }
            }

            // ← 从文件选择按钮（参照 Win）
            PickFromFileButton(
                text = "从文件选择",
                onClick = onPickFromFile,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            // 搜索框
            SearchBox(
                value = searchText,
                onValueChange = onSearchChange,
                placeholder = "搜索音频名称…",
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalPadding = 0.dp
            )

            if (filteredItems.isEmpty()) {
                Box(Modifier.fillMaxWidth().height(120.dp).padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
                    Text("没有匹配的音频", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                // 底部状态提示（对应 Win FooterHint "已选择 x 个"）
                Text(
                    text = "已选择 ${selectedIds.size} 个",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                LazyColumn(
                    modifier = Modifier.fillMaxWidth().height(360.dp)
                ) {
                    items(filteredItems, key = { it.id }) { item ->
                        val checked = item.id in selectedIds
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .clickable { onToggleItem(item.id) }
                                .padding(vertical = 8.dp, horizontal = 20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = checked,
                                onCheckedChange = null,
                                colors = CheckboxDefaults.colors(
                                    checkedColor = AccentBlue,
                                    uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    disabledCheckedColor = AccentBlue,
                                    disabledUncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ))
                            Spacer(Modifier.width(8.dp))
                            Column {
                                // 来源资源名（小号、次要色）—— 参照 Win SourceName
                                Text(
                                    text = item.sourceName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                // 音频名（主色）—— 参照 Win Name
                                Text(
                                    text = item.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (item.duration > 0) {
                                    Text(
                                        text = formatAudioDuration(item.duration),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp).fillMaxWidth())
        }
    }
}
