package com.stand.sounder_app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

data class ContextMenuItem(
    val label: String,
    val icon: ImageVector? = null,
    val onClick: () -> Unit,
    val isDestructive: Boolean = false
)

@Composable
fun ContextMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    items: List<ContextMenuItem>,
    modifier: Modifier = Modifier
) {
    IosActionSheet(
        show = expanded,
        onDismiss = onDismiss,
        items = items.map { IosActionSheetItem(
            label = it.label,
            onClick = it.onClick,
            isDestructive = it.isDestructive
        )},
        modifier = modifier
    )
}
