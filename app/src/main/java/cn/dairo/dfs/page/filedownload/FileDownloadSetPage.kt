package cn.dairo.dfs.page.filedownload

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Timer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import cn.dairo.dfs.components.AlertButton
import cn.dairo.dfs.components.AppAlert
import cn.dairo.dfs.components.ButtonRole
import cn.dairo.dfs.store.SettingStore
import cn.dairo.dfs.util.FileSdkUtil
import cn.dairo.file.sdk.CacheMode
import cn.dairo.file.sdk.GroupStats
import cn.dairo.ui.setting.SettingButton
import cn.dairo.ui.setting.SettingGroup
import cn.dairo.ui.setting.SettingIcon
import cn.dairo.ui.setting.SettingPage
import cn.dairo.ui.setting.SettingPicker
import cn.dairo.ui.setting.SettingPickerData
import cn.dairo.ui.setting.SettingStack
import cn.dairo.ui.util.fileSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 同时下载文件数模版 */
private val COUNT_DATA = (1..10).map { SettingPickerData("$it", it) }

/** 缓存文件保存期限模版 */
private val CACHE_EXPIRE_DAY_DATA = listOf(
    SettingPickerData("1天", 1),
    SettingPickerData("1星期", 7),
    SettingPickerData("1个月(30天)", 30),
    SettingPickerData("3个月(90天)", 90),
    SettingPickerData("6个月(180天)", 180),
    SettingPickerData("1年(365天)", 365),
    SettingPickerData("永久", 0),
)

@Composable
fun FileDownloadSetPage() {

    /** 同时下载文件数 */
    var maxSavingCount by remember { mutableIntStateOf(if (SettingStore.fileSdkMaxSavingCount > 0) SettingStore.fileSdkMaxSavingCount else 3) }

    /** 同时缓存文件数 */
    var maxCachingCount by remember { mutableIntStateOf(if (SettingStore.fileSdkMaxCachingCount > 0) SettingStore.fileSdkMaxCachingCount else 5) }

    /** 缓存文件保存天数,0表示永久 */
    var cacheExpireDays by remember { mutableIntStateOf(SettingStore.fileSdkCacheExpireDays ?: 90) }

    /** 缓存分组统计 */
    var stats by remember { mutableStateOf(emptyList<GroupStats>()) }

    /** 要清理的缓存分组 */
    var clearGroup by remember { mutableStateOf<String?>(null) }

    /** 统计的版本号,改变时重新加载 */
    var statsVersion by remember { mutableIntStateOf(0) }
    LaunchedEffect(statsVersion) {
        stats = withContext(Dispatchers.IO) { runCatching { FileSdkUtil.get().stats(CacheMode.CACHE) }.getOrDefault(emptyList()) }
    }
    SettingStack(embedInNavigationStack = false) {
        SettingPage(title = "下载设置") {
            SettingGroup {
                SettingPicker(
                    "同时下载文件数",
                    data = COUNT_DATA,
                    value = maxSavingCount,
                    onValueChange = {
                        maxSavingCount = it
                        SettingStore.fileSdkMaxSavingCount = it
                        runCatching { FileSdkUtil.get().setMaxSavingCount(it.toUInt()) }
                    },
                    icon = SettingIcon.System(Icons.Default.Download, backgroundColor = Color(0xFFFF3B30)),
                )
                SettingPicker(
                    "同时缓存文件数",
                    data = COUNT_DATA,
                    value = maxCachingCount,
                    onValueChange = {
                        maxCachingCount = it
                        SettingStore.fileSdkMaxCachingCount = it
                        runCatching { FileSdkUtil.get().setMaxCachingCount(it.toUInt()) }
                    },
                    icon = SettingIcon.System(Icons.Default.Download, backgroundColor = Color(0xFF34C759)),
                )
            }
            SettingGroup {
                SettingPicker(
                    "缓存文件保存期限",
                    data = CACHE_EXPIRE_DAY_DATA,
                    value = cacheExpireDays,
                    onValueChange = {
                        cacheExpireDays = it
                        SettingStore.fileSdkCacheExpireDays = it
                        runCatching { FileSdkUtil.get().setCacheExpireDays(it.toUInt()) }
                    },
                    icon = SettingIcon.System(Icons.Default.Timer, backgroundColor = Color(0xFFFF9500)),
                )
            }
            if (stats.isNotEmpty()) {
                SettingGroup {
                    for (it in stats) {
                        SettingButton(
                            groupLabel(it.group),
                            tip = "${it.count}个文件  " + it.size.toLong().fileSize,
                            icon = SettingIcon.System(Icons.Default.CalendarMonth, backgroundColor = Color(0xFF32ADE6)),
                        ) {
                            clearGroup = it.group
                        }
                    }
                }
            }
        }
    }
    val group = clearGroup
    AppAlert(
        isPresented = group != null,
        title = "确认清理这些缓存吗？",
        message = "此操作无法撤销",
        onDismiss = { clearGroup = null },
        buttons = listOf(
            AlertButton("清理", ButtonRole.DESTRUCTIVE) {
                runCatching { FileSdkUtil.get().clearCache(group, null) }
                statsVersion += 1
            },
            AlertButton("取消", ButtonRole.CANCEL),
        ),
    )
}

/** 缓存分组的显示名 */
private fun groupLabel(group: String): String {
    return when (group) {
        "file" -> "原文件"
        "thumb" -> "缩略图"
        "avatar" -> "头像"
        "preview" -> "预览文件"
        "thumb-online" -> "在线生成的图片"
        "preview-dlive" -> "实况预览视频"
        "dlive" -> "实况视频"
        "property" -> "文件属性"
        else -> group
    }
}
