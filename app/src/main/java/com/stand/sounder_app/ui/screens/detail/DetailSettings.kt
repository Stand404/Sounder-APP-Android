package com.stand.sounder_app.ui.screens.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stand.sounder_app.R
import com.stand.sounder_app.data.model.LoopMode
import com.stand.sounder_app.data.model.OrderMode
import com.stand.sounder_app.data.model.PlayMode
import com.stand.sounder_app.ui.components.PillOption
import com.stand.sounder_app.ui.components.PillToggle

@Composable
fun ResourceSettingSection(
    playMode: PlayMode,
    orderMode: OrderMode,
    loopMode: LoopMode = LoopMode.SINGLE,
    onPlayModeChange: (PlayMode) -> Unit,
    onOrderModeChange: (OrderMode) -> Unit,
    onLoopModeChange: (LoopMode) -> Unit = {}
) {
    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        // 播放模式
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.play_mode_label), fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Text(
                    text = when (playMode) {
                        PlayMode.OVERLAY -> stringResource(R.string.overlay_desc)
                        PlayMode.REPLACE -> stringResource(R.string.replace_desc)
                        PlayMode.LOOP -> stringResource(R.string.loop_desc)
                    },
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
            PillToggle(
                options = listOf(
                    PillOption(PlayMode.OVERLAY, stringResource(R.string.overlay_mode)),
                    PillOption(PlayMode.REPLACE, stringResource(R.string.replace_mode)),
                    PillOption(PlayMode.LOOP, stringResource(R.string.loop_mode))
                ),
                selectedValue = playMode,
                onSelect = onPlayModeChange
            )
        }

        Spacer(Modifier.height(8.dp))

        // 循环模式细分（仅循环模式可见）
        if (playMode == PlayMode.LOOP) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.loop_type_label), fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    Text(
                        text = stringResource(R.string.loop_type_desc),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
                PillToggle(
                    options = listOf(
                        PillOption(LoopMode.SINGLE, stringResource(R.string.loop_single)),
                        PillOption(LoopMode.LIST, stringResource(R.string.loop_list))
                    ),
                    selectedValue = loopMode,
                    onSelect = onLoopModeChange
                )
            }

            Spacer(Modifier.height(8.dp))
        }

        // 播放顺序
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.play_order_label), fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Text(
                    text = stringResource(R.string.order_desc),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
            PillToggle(
                options = listOf(
                    PillOption(OrderMode.ORDER, stringResource(R.string.order_play)),
                    PillOption(OrderMode.RANDOM, stringResource(R.string.random_play))
                ),
                selectedValue = orderMode,
                onSelect = onOrderModeChange
            )
        }
    }
}

@Composable
fun InfoPill(icon: @Composable () -> Unit, text: String) {
    Surface(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            icon()
            Text(
                text = text,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
