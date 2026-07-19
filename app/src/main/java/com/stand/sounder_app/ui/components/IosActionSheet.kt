package com.stand.sounder_app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stand.sounder_app.R
import com.stand.sounder_app.ui.theme.AccentBlue
import com.stand.sounder_app.ui.theme.ErrorRed
import kotlinx.coroutines.launch

data class IosActionSheetItem(
    val label: String,
    val onClick: () -> Unit,
    val isDestructive: Boolean = false,
    val isBold: Boolean = false
)

/**
 * iOS 风格底部弹窗（Action Sheet）
 *
 * 点击取消或动作项时先执行退场动画，再调用 onDismiss。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IosActionSheet(
    modifier: Modifier = Modifier,
    show: Boolean,
    onDismiss: () -> Unit,
    title: String? = null,
    message: String? = null,
    items: List<IosActionSheetItem>,
    cancelLabel: String = ""
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val cancelText = cancelLabel.ifBlank { stringResource(R.string.cancel) }

    // show=true 时确保 sheet 展开；show=false 时触发退场动画再回调 onDismiss
    LaunchedEffect(show) {
        if (show) {
            sheetState.show()
        } else {
            sheetState.hide()
        }
    }

    if (!show) return

    ModalBottomSheet(
        onDismissRequest = {
            scope.launch {
                sheetState.hide()
                onDismiss()
            }
        },
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp),
        containerColor = Color.Transparent,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            // ======== 操作区（含标题 + 动作项，白色背景） ========
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 标题（可选）
                    if (title != null) {
                        Spacer(Modifier.height(18.dp))
                        Text(
                            text = title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (message != null) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = message,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider(
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }

                    // 动作项
                    items.forEachIndexed { index, item ->
                        ActionSheetButton(
                            item = item,
                            onDismiss = {
                                scope.launch {
                                    // 先播放退场动画（此时 show 仍为 true，sheet 仍在组合中，
                                    // 动画期间不会与后续打开的弹窗构成两个 ModalBottomSheet 同屏），
                                    // 动画结束后再置显隐状态为 false，最后执行动作。
                                    // 待删除 id 由调用方用独立状态保存，onDismiss 置空 show 不影响动作读取。
                                    sheetState.hide()
                                    onDismiss()
                                    item.onClick()
                                }
                            }
                        )
                        if (index < items.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ======== 取消按钮（独立圆角容器） ========
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable {
                        scope.launch {
                            sheetState.hide()
                            onDismiss()
                        }
                    }
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = cancelText,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

    @Composable
private fun ActionSheetButton(
    item: IosActionSheetItem,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onDismiss() }
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = item.label,
            fontSize = 18.sp,
            fontWeight = if (item.isBold) FontWeight.Bold else FontWeight.Normal,
            color = if (item.isDestructive) ErrorRed else AccentBlue,
            textAlign = TextAlign.Center
        )
    }
}
