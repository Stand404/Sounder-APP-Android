package com.stand.sounder_app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.ripple
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stand.sounder_app.R
import com.stand.sounder_app.data.repository.SettingsRepository
import com.stand.sounder_app.ui.components.IosActionSheet
import com.stand.sounder_app.ui.components.IosActionSheetItem
import com.stand.sounder_app.ui.components.PillOption
import com.stand.sounder_app.ui.components.PillToggle
import com.stand.sounder_app.ui.screens.settings.SettingsHeader
import com.stand.sounder_app.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel()
) {
    val themeMode by viewModel.themeMode.collectAsState()
    val currentLanguage by viewModel.language.collectAsState()
    val cacheSizeText by viewModel.cacheSizeText.collectAsState()

    var showClearCacheDialog by remember { mutableStateOf(false) }
    var showLanguageSheet by remember { mutableStateOf(false) }

    val themeOptions = listOf(
        PillOption(SettingsRepository.THEME_AUTO, stringResource(R.string.auto_theme)),
        PillOption(SettingsRepository.THEME_LIGHT, stringResource(R.string.light)),
        PillOption(SettingsRepository.THEME_DARK, stringResource(R.string.dark))
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
            SettingsHeader()
            // ===== 设置列表 =====
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 12.dp)
                    .padding(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            ) {
                Column {
                    // 1. 主题模式
                    SettingsRow(
                        icon = { Icon(painterResource(R.drawable.ic_theme), null, tint = MaterialTheme.colorScheme.primary) },
                        title = stringResource(R.string.theme_mode),
                        trailing = {
                            PillToggle(
                                options = themeOptions,
                                selectedValue = themeMode,
                                onSelect = { viewModel.setThemeMode(it) },
                                modifier = Modifier.width(180.dp)
                            )
                        }
                    )
                    Divider()
                    // 2. 语言设置
                    val languageLabels = mapOf(
                        SettingsRepository.LANG_SYSTEM to stringResource(R.string.language_system),
                        SettingsRepository.LANG_ZH to stringResource(R.string.language_zh),
                        SettingsRepository.LANG_ZH_TW to stringResource(R.string.language_zh_tw),
                        SettingsRepository.LANG_EN to stringResource(R.string.language_en),
                        SettingsRepository.LANG_JA to stringResource(R.string.language_ja),
                        SettingsRepository.LANG_RU to stringResource(R.string.language_ru),
                    )
                    SettingsRow(
                        icon = { Icon(painterResource(R.drawable.ic_language), null, tint = MaterialTheme.colorScheme.primary) },
                        title = stringResource(R.string.language_setting),
                        trailing = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f))
                                    .clickable { showLanguageSheet = true }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = languageLabels[currentLanguage] ?: stringResource(R.string.language_system),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.width(4.dp))
                                Icon(
                                    painter = painterResource(R.drawable.ic_arrow_right),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    )
                    Divider()
                    // 3. 缓存清理
                    SettingsRow(
                        icon = { Icon(painterResource(R.drawable.ic_broom), null, tint = MaterialTheme.colorScheme.primary) },
                        title = stringResource(R.string.cache_cleanup),
                        trailing = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f))
                                    .clickable { showClearCacheDialog = true }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = cacheSizeText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.width(4.dp))
                                Icon(
                                    painter = painterResource(R.drawable.ic_arrow_right),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    )
                    Divider()
                    // 3. 进入官网
                    SettingsRow(
                        icon = { Icon(painterResource(R.drawable.ic_link), null, tint = MaterialTheme.colorScheme.primary) },
                        title = stringResource(R.string.enter_official_site),
                        trailing = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f))
                                    .clickable { viewModel.openUrl(viewModel.websiteUrl) }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.get_fan_group),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.width(4.dp))
                                Icon(
                                    painter = painterResource(R.drawable.ic_arrow_right),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    )
                    Divider()
                    // 4. 作者
                    SettingsRow(
                        icon = { Icon(painterResource(R.drawable.ic_person), null, tint = MaterialTheme.colorScheme.primary) },
                        title = stringResource(R.string.author_name),
                        trailing = {
                            Text(
                                text = viewModel.authorName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }
                    )
                }
            }

            // ===== 关于 =====
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(R.drawable.ic_info),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.about_title),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    // 版本卡片1：发声APP
                    AboutCard(
                        image = painterResource(R.drawable.sounder),
                        title = stringResource(R.string.app_name_display),
                        badge = stringResource(R.string.badge_android),
                        badgeBackground = MaterialTheme.colorScheme.primary,
                        description = stringResource(R.string.app_description)
                    )

                    // 版本卡片2：发声APP · 造化版
                    AboutCard(
                        image = painterResource(R.drawable.ico),
                        title = stringResource(R.string.app_name_zaohua),
                        badge = stringResource(R.string.badge_multi_platform),
                        badgeBackground = Color.Transparent,
                        badgeGradient = Brush.linearGradient(
                            colors = listOf(Color(0xFFF7971E), Color(0xFFFFD200))
                        ),
                        description = stringResource(R.string.app_full_description)
                    )

                    // 版本卡片3：独立APP整合包获取
                    LinkCard(
                        image = painterResource(R.drawable.blue_android),
                        title = stringResource(R.string.independent_package),
                        badge = stringResource(R.string.badge_android),
                        badgeBackground = MaterialTheme.colorScheme.primary,
                        description = stringResource(R.string.independent_desc),
                        onClick = { viewModel.openUrl(viewModel.packageUrl) }
                    )

                    // 版本卡片4：纯音频文件获取
                    LinkCard(
                        image = painterResource(R.drawable.audio_files),
                        title = stringResource(R.string.audio_files),
                        description = stringResource(R.string.audio_files_desc),
                        onClick = { viewModel.openUrl(viewModel.audioFilesUrl) }
                    )

                    // 隐私声明
                    NoteRow(
                        icon = { Icon(painterResource(R.drawable.ic_eye), null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                        text = stringResource(R.string.privacy_notice)
                    )
                    // 反馈提示
                    NoteRow(
                        icon = { Icon(painterResource(R.drawable.ic_chat), null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                        text = stringResource(R.string.feedback_notice)
                    )
                }
            }
        }
    }

    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            title = { Text(stringResource(R.string.clear_cache_confirm_title)) },
            text = { Text(stringResource(R.string.clear_cache_confirm_msg)) },
            confirmButton = {
                TextButton(onClick = {
                    showClearCacheDialog = false
                    viewModel.clearCache()
                }) {
                    Text(stringResource(R.string.clear))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    IosActionSheet(
        show = showLanguageSheet,
        onDismiss = { showLanguageSheet = false },
        items = listOf(
            IosActionSheetItem(label = stringResource(R.string.language_system), onClick = { viewModel.setLanguage(SettingsRepository.LANG_SYSTEM) }),
            IosActionSheetItem(label = stringResource(R.string.language_zh), onClick = { viewModel.setLanguage(SettingsRepository.LANG_ZH) }),
            IosActionSheetItem(label = stringResource(R.string.language_zh_tw), onClick = { viewModel.setLanguage(SettingsRepository.LANG_ZH_TW) }),
            IosActionSheetItem(label = stringResource(R.string.language_en), onClick = { viewModel.setLanguage(SettingsRepository.LANG_EN) }),
            IosActionSheetItem(label = stringResource(R.string.language_ja), onClick = { viewModel.setLanguage(SettingsRepository.LANG_JA) }),
            IosActionSheetItem(label = stringResource(R.string.language_ru), onClick = { viewModel.setLanguage(SettingsRepository.LANG_RU) }),
        )
    )
}

}

@Composable
private fun SettingsRow(
    icon: @Composable () -> Unit,
    title: String,
    trailing: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()
        Spacer(Modifier.width(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.weight(1f))
        trailing()
    }
}

@Composable
private fun Divider() {
    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .height(0.5.dp)
            .padding(start = 16.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    )
}

@Composable
private fun AboutCard(
    image: Painter,
    title: String,
    badge: String? = null,
    badgeBackground: Color = Color.Transparent,
    badgeGradient: Brush? = null,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Image(
                painter = image,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    if (badge != null) {
                        BadgeChip(text = badge, background = badgeBackground, gradient = badgeGradient)
                    }
                }
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LinkCard(
    image: Painter,
    title: String,
    badge: String? = null,
    badgeBackground: Color = Color.Transparent,
    description: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = image,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    if (badge != null) {
                        BadgeChip(text = badge, background = badgeBackground)
                    }
                }
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                painter = painterResource(R.drawable.ic_arrow_right),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BadgeChip(
    text: String,
    background: Color,
    gradient: Brush? = null
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = gradient?.let { Color.Transparent } ?: background
    ) {
        Box(
            modifier = if (gradient != null) Modifier.background(gradient) else Modifier,
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun NoteRow(
    icon: @Composable () -> Unit,
    text: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.padding(top = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(modifier = Modifier.padding(top = 2.dp)) { icon() }
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 16.sp
        )
    }
}
