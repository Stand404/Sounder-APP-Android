package com.stand.sounder_app.ui.theme

import androidx.compose.ui.graphics.Color

// ===== 设计令牌（参考 docs/01-global-styles.md，跨平台保持一致） =====

// --- 二级视觉背景（Light） ---
val Lv1Bg = Color(0xFFF5F6FA)         // 页面底色
val Lv2Bg = Color(0xFFFFFFFF)         // 卡片/区块/面板背景
val Lv2Border = Color(0xFFE2E5EB)     // 区块边框
val Lv2Hover = Color(0xFFEEF1F7)      // 悬停态（移动端用 press 替代）

// --- 文本色（Light） ---
val TextPrimary = Color(0xFF1D1F27)
val TextSecondary = Color(0xFF6B7280)
val TextTertiary = Color(0xFF9CA3AF)
val LabelText = Color(0xFF82858F)

// --- 强调色（Light，核心品牌色必须跨平台一致 = #5DA3E8） ---
val AccentBlue = Color(0xFF5DA3E8)
val AccentBlueHover = Color(0xFF4A8AD0)
val AccentBluePressed = Color(0xFF7DBAF0)
val BtnFgSoft = Color(0xFFFFFFFF)     // 深色按钮文字色

// --- 错误/危险色（Light） ---
val ErrorRed = Color(0xFFEF4444)
val ErrorRedHover = Color(0xFFDC2626)

// --- 选中/播放高亮（Light） ---
val SelectBg = Color(0xFFEDF4FC)
val PlayingHighlightBg = Color(0x145DA3E8)    // AccentBlue 8% 透明

// --- 侧边栏/导航背景（Light） ---
val SidebarBg = Color(0xFFF8F9FC)

// --- 状态色（Light） ---
val StatusInstalledBg = Color(0xFFECFDF5)
val StatusInstalledText = Color(0xFF059669)
val StatusUninstalledBg = Color(0xFFF3F4F6)
val StatusUninstalledText = Color(0xFF8B8F98)

// ===== Material3 浅色映射 =====
val Primary = AccentBlue
val OnPrimary = BtnFgSoft
val PrimaryContainer = SelectBg
val OnPrimaryContainer = Color(0xFF001D36)

val Secondary = TextSecondary
val SecondaryContainer = Color(0xFFD7E3F7)
val OnSecondaryContainer = Color(0xFF101C2B)

val Background = Lv1Bg
val OnBackground = TextPrimary
val Surface = Lv2Bg
val OnSurface = TextPrimary
val SurfaceVariant = Lv2Border
val OnSurfaceVariant = TextSecondary

val Error = ErrorRed
val ErrorContainer = Color(0xFFFFDAD6)

// ===== 二级视觉背景（Dark） =====
val DarkLv1Bg = Color(0xFF0D0D0D)
val DarkLv2Bg = Color(0xFF181818)     // 卡片/区块背景（原来的 Lv3）
val DarkLv2Border = Color(0xFF2A2A2A)
val DarkLv2Hover = Color(0xFF202020)

// --- 文本色（Dark） ---
val DarkTextPrimary = Color(0xFFE6E6E6)
val DarkTextSecondary = Color(0xFF8E8E8E)
val DarkTextTertiary = Color(0xFF5E5E5E)
val DarkLabelText = Color(0xFF767676)

// --- 强调色（Dark，与浅色主题相同） ---
val DarkAccentBlue = Color(0xFF5DA3E8)

// --- 错误/危险色（Dark，稍亮） ---
val DarkErrorRed = Color(0xFFEF5350)

// --- 选中/播放高亮（Dark） ---
val DarkPlayingHighlightBg = Color(0x285DA3E8)   // AccentBlue 16% 透明

// --- 侧边栏/导航背景（Dark） ---
val DarkSidebarBg = Color(0xFF0A0A0A)

// --- 状态色（Dark） ---
val DarkStatusInstalledBg = Color(0xFF1A3422)
val DarkStatusInstalledText = Color(0xFF5CB860)
val DarkStatusUninstalledBg = Color(0xFF242424)
val DarkStatusUninstalledText = Color(0xFF7A7A7A)

// ===== Material3 暗色映射 =====
val DarkPrimary = DarkAccentBlue
val DarkOnPrimary = BtnFgSoft
val DarkPrimaryContainer = Color(0xFF00497D)
val DarkOnPrimaryContainer = Color(0xFFD3E4FD)

val DarkSecondary = Color(0xFFBBC7DB)
val DarkSecondaryContainer = Color(0xFF3B4858)
val DarkOnSecondaryContainer = Color(0xFFD7E3F7)

val DarkBackground = DarkLv1Bg
val DarkOnBackground = DarkTextPrimary
val DarkSurface = DarkLv2Bg
val DarkOnSurface = DarkTextPrimary
val DarkSurfaceVariant = DarkLv2Border
val DarkOnSurfaceVariant = DarkTextSecondary

val DarkError = DarkErrorRed
val DarkErrorContainer = Color(0xFF93000A)
