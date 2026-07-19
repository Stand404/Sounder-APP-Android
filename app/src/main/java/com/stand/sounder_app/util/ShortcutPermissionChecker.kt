package com.stand.sounder_app.util

import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.text.TextUtils
import java.lang.reflect.Method
import androidx.core.net.toUri

/**
 * 检测「创建桌面快捷方式」权限在各厂商 ROM 上的授予情况。
 *
 * 华为 / 小米 / OPPO / Vivo 将该权限设计为运行时权限且默认禁止，检测方法各不相同，
 * 且无法通过运行时权限请求，只能引导用户到系统设置开启。
 *
 * 各厂商实现参考 https://github.com/scauzhangpeng/Shortcut （Android 7-11 验证通过）：
 *  - 华为：反射 com.huawei.hsm.permission.PermissionManager#canSendBroadcast
 *  - 小米：反射 AppOpsManager#checkOpNoThrow，op = 10017 (OP_INSTALL_SHORTCUT)
 *  - OPPO：ContentProvider 查询 content://settings/secure/launcher_shortcut_permission_settings
 *  - Vivo：ContentProvider 查询 content://com.bbk.launcher2.settings/favorites
 */
enum class ShortcutPermState {
    GRANTED,  // 已允许
    DENIED,   // 已拒绝 / 禁用
    ASK,      // 需要询问 / 请求
    UNKNOWN   // 未知（未适配厂商，按系统默认行为处理，直接尝试）
}

object ShortcutPermissionChecker {

    fun check(context: Context): ShortcutPermState {
        val mark = Build.MANUFACTURER.lowercase()
        return when {
            mark.contains("huawei") -> checkOnEMUI(context)
            mark.contains("xiaomi") -> checkOnMIUI(context)
            mark.contains("oppo") -> checkOnOPPO(context)
            mark.contains("vivo") -> checkOnVIVO(context)
            // 原生 Android / 三星 / 魅族不需要单独授权，声明权限即可
            mark.contains("samsung") || mark.contains("meizu") -> ShortcutPermState.GRANTED
            else -> ShortcutPermState.UNKNOWN
        }
    }

    fun checkOnEMUI(context: Context): ShortcutPermState {
        val intent = Intent("com.android.launcher.action.INSTALL_SHORTCUT")
        return try {
            val permissionManagerClass = Class.forName("com.huawei.hsm.permission.PermissionManager")
            val canSendBroadcast: Method = permissionManagerClass.getDeclaredMethod(
                "canSendBroadcast", Context::class.java, Intent::class.java
            )
            val invoke = canSendBroadcast.invoke(permissionManagerClass, context, intent)
            if (invoke != null) {
                val result = invoke as Boolean
                if (result) ShortcutPermState.GRANTED else ShortcutPermState.DENIED
            } else {
                ShortcutPermState.UNKNOWN
            }
        } catch (e: Throwable) {
            ShortcutPermState.UNKNOWN
        }
    }

    fun checkOnVIVO(context: Context): ShortcutPermState {
        val contentResolver = context.contentResolver ?: return ShortcutPermState.UNKNOWN
        var query: Cursor? = null
        return try {
            val uri = "content://com.bbk.launcher2.settings/favorites".toUri()
            query = contentResolver.query(uri, null, null, null, null)
            if (query == null) {
                ShortcutPermState.UNKNOWN
            } else {
                val titleIndex = query.getColumnIndex("title")
                val permissionIndex = query.getColumnIndex("shortcutPermission")
                if (titleIndex < 0 || permissionIndex < 0) return ShortcutPermState.UNKNOWN

                val appName = getAppName(context)
                var state = ShortcutPermState.UNKNOWN
                while (query.moveToNext()) {
                    val title = query.getString(titleIndex)
                    if (!TextUtils.isEmpty(title) && title == appName) {
                        val value = query.getInt(permissionIndex)
                        state = when (value) {
                            1, 17 -> ShortcutPermState.DENIED
                            16 -> ShortcutPermState.GRANTED
                            18 -> ShortcutPermState.ASK
                            else -> ShortcutPermState.UNKNOWN
                        }
                        break
                    }
                }
                state
            }
        } catch (e: Throwable) {
            ShortcutPermState.UNKNOWN
        } finally {
            query?.close()
        }
    }

    fun checkOnMIUI(context: Context): ShortcutPermState {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) return ShortcutPermState.UNKNOWN
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val pkg = context.applicationContext.packageName
            val uid = context.applicationInfo.uid
            val appOpsClass = Class.forName(AppOpsManager::class.java.name)
            val checkOpNoThrow: Method = appOpsClass.getDeclaredMethod(
                "checkOpNoThrow",
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                String::class.java
            )
            // OP_INSTALL_SHORTCUT = 10017（小米私有 op code）
            val invoke = checkOpNoThrow.invoke(appOps, 10017, uid, pkg)
            when (invoke?.toString()) {
                "0" -> ShortcutPermState.GRANTED
                "1" -> ShortcutPermState.DENIED
                "5" -> ShortcutPermState.ASK
                else -> ShortcutPermState.UNKNOWN
            }
        } catch (e: Throwable) {
            ShortcutPermState.UNKNOWN
        }
    }

    fun checkOnOPPO(context: Context): ShortcutPermState {
        val contentResolver = context.contentResolver ?: return ShortcutPermState.UNKNOWN
        val uri = Uri.parse("content://settings/secure/launcher_shortcut_permission_settings")
        val query = contentResolver.query(uri, null, null, null, null) ?: return ShortcutPermState.UNKNOWN
        return try {
            val valueIndex = query.getColumnIndex("value")
            if (valueIndex < 0) return ShortcutPermState.UNKNOWN

            val pkg = context.applicationContext.packageName
            var state = ShortcutPermState.UNKNOWN
            while (query.moveToNext()) {
                val value = query.getString(valueIndex) ?: continue
                if (value.contains("$pkg, 1")) {
                    state = ShortcutPermState.GRANTED
                    break
                }
                if (value.contains("$pkg, 0")) {
                    state = ShortcutPermState.DENIED
                    break
                }
            }
            state
        } catch (e: Throwable) {
            ShortcutPermState.UNKNOWN
        } finally {
            query.close()
        }
    }

    private fun getAppName(context: Context): String {
        return try {
            val pm = context.packageManager
            val pi: PackageInfo = pm.getPackageInfo(context.applicationContext.packageName, 0)
            pi.applicationInfo?.loadLabel(pm)?.toString() ?: ""
        } catch (e: PackageManager.NameNotFoundException) {
            ""
        }
    }

    /**
     * 跳转到本应用的「创建桌面快捷方式」权限设置页。
     * 该权限无法用运行时权限请求，只能引导用户手动开启。
     */
    fun openShortcutSettings(context: Context) {
        val mark = Build.MANUFACTURER.lowercase()
        val intent = when {
            mark.contains("huawei") -> huaweiApi()
            mark.contains("xiaomi") -> xiaomiApi(context)
            mark.contains("oppo") -> oppoApi(context)
            mark.contains("vivo") -> vivoApi(context)
            mark.contains("meizu") -> meizuApi(context)
            else -> defaultApi(context)
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (e: Throwable) {
            context.startActivity(defaultApi(context))
        }
    }

    private fun defaultApi(context: Context): Intent {
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
    }

    private fun huaweiApi(): Intent {
        return Intent().apply {
            component = ComponentName(
                "com.huawei.systemmanager",
                "com.huawei.permissionmanager.ui.MainActivity"
            )
        }
    }

    private fun xiaomiApi(context: Context): Intent {
        val version = getSystemProperty("ro.miui.ui.version.name")
        if (version.isNullOrEmpty()) return defaultApi(context)
        val versionI = try {
            version.substring(1).toInt()
        } catch (e: NumberFormatException) {
            return defaultApi(context)
        }
        return if (versionI >= 9) {
            Intent("miui.intent.action.APP_PERM_EDITOR").apply {
                component = ComponentName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.permissions.PermissionsEditorActivity"
                )
                putExtra("extra_pkgname", context.packageName)
            }
        } else if (versionI >= 7) {
            Intent("miui.intent.action.APP_PERM_EDITOR").apply {
                putExtra("extra_pkgname", context.packageName)
            }
        } else {
            defaultApi(context)
        }
    }

    private fun vivoApi(context: Context): Intent {
        val intent = Intent().apply {
            putExtra("packagename", context.packageName)
            component = ComponentName(
                "com.vivo.permissionmanager",
                "com.vivo.permissionmanager.activity.SoftPermissionDetailActivity"
            )
        }
        if (hasActivity(context, intent)) return intent
        intent.component = ComponentName(
            "com.iqoo.secure",
            "com.iqoo.secure.safeguard.SoftPermissionDetailActivity"
        )
        return intent
    }

    private fun oppoApi(context: Context): Intent {
        val intent = Intent().apply {
            putExtra("packageName", context.packageName)
            setClassName(
                "com.oppo.launcher",
                "com.oppo.launcher.shortcut.ShortcutSettingsActivity"
            )
        }
        if (hasActivity(context, intent)) return intent
        intent.component = ComponentName(
            "com.color.safecenter",
            "com.color.safecenter.permission.PermissionManagerActivity"
        )
        return intent
    }

    private fun meizuApi(context: Context): Intent {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) return defaultApi(context)
        return Intent("com.meizu.safe.security.SHOW_APPSEC").apply {
            putExtra("packageName", context.packageName)
            component = ComponentName(
                "com.meizu.safe",
                "com.meizu.safe.security.AppSecActivity"
            )
        }
    }

    private fun hasActivity(context: Context, intent: Intent): Boolean {
        val pm = context.packageManager
        return pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY).isNotEmpty()
    }

    private fun getSystemProperty(propName: String): String? {
        return try {
            val p = Runtime.getRuntime().exec("getprop $propName")
            p.inputStream.bufferedReader().readLine()
        } catch (e: Throwable) {
            null
        }
    }
}
