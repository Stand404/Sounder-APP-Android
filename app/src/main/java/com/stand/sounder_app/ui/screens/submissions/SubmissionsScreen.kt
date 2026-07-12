package com.stand.sounder_app.ui.screens.submissions

import android.content.ClipboardManager
import android.content.ClipData
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.stand.sounder_app.ui.components.EmptyState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stand.sounder_app.R
import com.stand.sounder_app.data.model.Submission
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import com.stand.sounder_app.data.model.SubmissionStatus
import com.stand.sounder_app.ui.components.PillOption
import com.stand.sounder_app.viewmodel.SubmissionsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubmissionsScreen(
    onNewSubmission: () -> Unit,
    viewModel: SubmissionsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val context = LocalContext.current

    val shouldLoadMore by remember {
        derivedStateOf {
            if (uiState.isLoading || uiState.submissions.isEmpty()) false
            else {
                val info = listState.layoutInfo.visibleItemsInfo
                if (info.isEmpty()) false
                else info.last().index >= listState.layoutInfo.totalItemsCount - 2
            }
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && uiState.hasMore && !uiState.isLoadingMore) {
            viewModel.loadMore()
        }
    }

    val statusOptions: List<PillOption<SubmissionStatus>> = listOf(
                        PillOption(SubmissionStatus.IN_PROGRESS, stringResource(R.string.status_in_progress)),
                        PillOption(SubmissionStatus.PENDING, stringResource(R.string.status_pending)),
                        PillOption(SubmissionStatus.COMPLETED, stringResource(R.string.status_completed))
    )
    val tabIndicatorColors = listOf(
        Color(SubmissionStatus.IN_PROGRESS.sideColor),
        Color(SubmissionStatus.PENDING.sideColor),
        Color(SubmissionStatus.COMPLETED.sideColor)
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets.systemBars.only(WindowInsetsSides.Top),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            SubmissionsHeader(
                statusOptions = statusOptions,
                selectedStatus = uiState.selectedStatus,
                tabIndicatorColors = tabIndicatorColors,
                keyword = uiState.keyword,
                onKeywordChange = { viewModel.setKeyword(it) },
                onNewSubmission = onNewSubmission,
                onStatusChange = { viewModel.setStatusFilter(it) }
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                when {
                uiState.isLoading && uiState.submissions.isEmpty() -> {
                    EmptyState(
                        modifier = Modifier.padding(bottom = 72.dp),
                        isLoading = true,
                        title = ""
                    )
                }
                uiState.error != null && uiState.submissions.isEmpty() -> {
                    EmptyState(
                        modifier = Modifier.padding(bottom = 72.dp),
                        isError = true,
                        title = uiState.error ?: stringResource(R.string.load_failed),
                        onRetry = { viewModel.loadSubmissions() }
                    )
                }
                uiState.submissions.isEmpty() -> {
                    EmptyState(
                        modifier = Modifier.padding(bottom = 72.dp),
                        title = stringResource(R.string.no_submissions),
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_calendar),
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    )
                }
                else -> {
                    androidx.compose.material3.pulltorefresh.PullToRefreshBox(
                        isRefreshing = uiState.isLoading,
                        onRefresh = { viewModel.loadSubmissions() },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(bottom = 72.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            item { Spacer(Modifier.height(4.dp)) }

                            if (uiState.isTimelineView) {
                                uiState.groupedSubmissions.forEach { group ->
                                    item(key = "group_header_${group.date}") {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = formatRelativeDate(group.date),
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                                    .padding(horizontal = 6.dp, vertical = 1.dp)
                                            ) {
                                                Text(
                                                    text = group.date,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                    items(group.items, key = { it.id }) { submission ->
                                        SubmissionCard(
                                            submission = submission,
                                            onCopyLink = { copyText(context, it) }
                                        )
                                    }
                                    item(key = "group_footer_${group.date}") {
                                        Spacer(Modifier.height(12.dp))
                                    }
                                }
                            } else {
                                items(uiState.submissions, key = { it.id }) { submission ->
                                    SubmissionCard(
                                        submission = submission,
                                        onCopyLink = { copyText(context, it) }
                                    )
                                }
                            }

                            item {
                                if (uiState.isLoadingMore) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.size(8.dp))
                                        Text(stringResource(R.string.load_more),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                } else if (!uiState.hasMore && !uiState.isLoading) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(stringResource(R.string.no_more_data),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                            item { Spacer(Modifier.height(80.dp)) }
                        }
                    }
                }
            }
            }
        }
    }
}

@Composable
private fun SubmissionCard(
    submission: Submission,
    onCopyLink: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val status = submission.statusEnum
    val sideColor = Color(status.sideColor)
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(250),
        label = "expandArrow"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 头部（点击展开/折叠）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(start = 8.dp, top = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 状态侧边色条
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(40.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(sideColor)
            )
            Spacer(modifier = Modifier.width(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = submission.resourceName.ifEmpty { stringResource(R.string.unnamed_submission) },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = status.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = sideColor
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                // 元信息行
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(R.drawable.ic_tag),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(submission.appName, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(12.dp))
                    Icon(
                        painter = painterResource(R.drawable.ic_person),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(submission.nickname, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(R.drawable.ic_calendar),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        formatSubmissionTime(submission.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    if (submission.platform.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                .padding(horizontal = 6.dp, vertical = 1.dp)
                        ) {
                            Text(
                                submission.platform,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
            Icon(
                painter = painterResource(R.drawable.ic_more),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(18.dp)
                    .rotate(rotation)
            )
        }

        // 展开详情
        if (expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
            ) {
                Divider()
                Spacer(modifier = Modifier.height(12.dp))
                DetailRow(label = stringResource(R.string.submission_form_brief), value = submission.brief)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        DetailRow(label = stringResource(R.string.submission_form_image_source), value = submission.imageSource)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        DetailRow(label = stringResource(R.string.submission_form_voice_source), value = submission.voiceSource)
                    }
                }
                if (!submission.imageSourceLink.isNullOrBlank()) {
                    LinkRow(label = stringResource(R.string.submission_form_image_source_link), link = submission.imageSourceLink, onCopyLink = onCopyLink)
                }
                if (!submission.voiceSourceLink.isNullOrBlank()) {
                    LinkRow(label = stringResource(R.string.submission_form_voice_source_link), link = submission.voiceSourceLink, onCopyLink = onCopyLink)
                }
                if (!submission.fileLink.isNullOrBlank()) {
                    DetailRow(label = stringResource(R.string.submission_form_notes), value = submission.fileLink)
                }
                if (!submission.platformId.isNullOrBlank()) {
                    DetailRow(label = stringResource(R.string.submission_form_platform_id), value = submission.platformId)
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    if (value.isBlank()) return
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun LinkRow(label: String, link: String, onCopyLink: (String) -> Unit) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                link,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            Spacer(modifier = Modifier.width(4.dp))
            IconButton(
                onClick = { onCopyLink(link) },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_clone),
                    contentDescription = stringResource(R.string.copy_link),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun Divider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
    )
}

/**
 * 将日期字符串格式化为相对描述（今天/昨天/前天/N天后/N天前），解析失败则原样返回。
 * 仅在 Composable 中调用，以便使用 stringResource 获取多语言文本。
 */
@Composable
private fun formatRelativeDate(dateStr: String): String {
    if (dateStr.isBlank()) return ""
    val sdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    val date = remember(dateStr) { runCatching { sdf.parse(dateStr) }.getOrNull() } ?: return dateStr
    val today = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
    }
    val target = remember(date) {
        Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
    }
    val diffDays = ((target.timeInMillis - today.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()
    return when {
        diffDays == 0 -> stringResource(R.string.date_today)
        diffDays == -1 -> stringResource(R.string.date_yesterday)
        diffDays == -2 -> stringResource(R.string.date_day_before_yesterday)
        diffDays == 1 -> stringResource(R.string.date_tomorrow)
        diffDays == 2 -> stringResource(R.string.date_day_after_tomorrow)
        diffDays > 2 -> stringResource(R.string.date_days_after, diffDays)
        else -> stringResource(R.string.date_days_before, -diffDays)
    }
}

/**
 * 将 ISO 8601 时间（如 2026-06-14T11:34:32+08:00）格式化为 yyyy-MM-dd HH:mm。
 * 无法解析时回退为原字符串。
 */
private fun formatSubmissionTime(raw: String?): String {
    if (raw.isNullOrBlank()) return ""
    val match = Regex("""(\d{4}-\d{2}-\d{2})[Tt ](\d{2}:\d{2})""").find(raw)
    return if (match != null) "${match.groupValues[1]} ${match.groupValues[2]}" else raw
}

private fun copyText(context: Context, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    cm?.setPrimaryClip(ClipData.newPlainText("link", text))
    Toast.makeText(context, context.getString(R.string.submission_link_copied), Toast.LENGTH_SHORT).show()
}
