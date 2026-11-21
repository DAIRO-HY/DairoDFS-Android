package cn.dairo.dfs.page.filedownload

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cn.dairo.dfs.components.AlertButton
import cn.dairo.dfs.components.AppAlert
import cn.dairo.dfs.components.ButtonRole
import cn.dairo.dfs.components.NavigationPage
import cn.dairo.dfs.components.OptionButton
import cn.dairo.dfs.extension.primaryText
import cn.dairo.dfs.extension.secondaryText
import cn.dairo.dfs.util.FileSdkUtil
import cn.dairo.dfs.util.OnFileSdkEvent
import cn.dairo.dfs.util.rememberStateObject
import cn.dairo.file.sdk.CacheInfo
import cn.dairo.file.sdk.CacheMode
import cn.dairo.file.sdk.DownloadState
import cn.dairo.ui.setting.LocalSettingNavigator
import cn.dairo.ui.util.fileSize
import kotlin.math.max

/** 下载和缓存两个标签 */
private val MODES = listOf("下载" to CacheMode.SAVE, "缓存" to CacheMode.CACHE)

@Composable
fun FileDownloadPage() {
    val vm = rememberStateObject { FileDownloadViewModel() }
    val navigator = LocalSettingNavigator.current
    var showDeleteAlert by remember { mutableStateOf(false) }
    OnFileSdkEvent(vm::onFileSdkEvent)
    NavigationPage(title = "下载管理") {
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(8.dp)) {
            MODES.forEachIndexed { index, (label, mode) ->
                SegmentedButton(
                    selected = vm.mode == mode,
                    onClick = { vm.onModeClick(mode) },
                    shape = SegmentedButtonDefaults.itemShape(index, MODES.size),
                ) {
                    Text(label)
                }
            }
        }
        Box(Modifier.fillMaxWidth().weight(1f)) {

            //操作按钮浮在列表上面,列表底部留出按钮的位置
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 100.dp)) {
                items(vm.list, key = { it.id }) { item ->
                    ItemView(vm, item)
                }
            }
            Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(vertical = 8.dp)) {
                OptionButton("全选", Icons.Default.Checklist, onClick = vm::onCheckAllClick)
                OptionButton("全暂停", Icons.Default.PauseCircle, disabled = vm.mode != CacheMode.SAVE, onClick = vm::onPauseAllClick)
                OptionButton("全开始", Icons.Default.PlayCircle, disabled = vm.mode != CacheMode.SAVE, onClick = vm::onStartAllClick)
                OptionButton("删除(${vm.checked.size})", Icons.Default.Delete, disabled = vm.checked.isEmpty()) {
                    showDeleteAlert = true
                }
                OptionButton("设置", Icons.Default.Settings) {
                    navigator?.push { FileDownloadSetPage() }
                }
            }
        }
    }
    AppAlert(
        isPresented = showDeleteAlert,
        title = "确认删除吗？",
        message = "此操作无法撤销",
        onDismiss = { showDeleteAlert = false },
        buttons = listOf(
            AlertButton("删除", ButtonRole.DESTRUCTIVE, vm::onDeleteClick),
            AlertButton("取消", ButtonRole.CANCEL),
        ),
    )
}

/** 一行文件信息 */
@Composable
private fun ItemView(vm: FileDownloadViewModel, item: CacheInfo) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            if (item.id in vm.checked) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = Color.primaryText,
            modifier = Modifier.size(22.dp).clickable { vm.onCheckClick(item.id) },
        )
        Column(Modifier.weight(1f).padding(start = 10.dp)) {
            Text(item.name ?: item.id, style = MaterialTheme.typography.bodyLarge, color = Color.primaryText, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (item.state == DownloadState.FINISH) {
                Text((item.size ?: item.downloaded).toLong().fileSize, style = MaterialTheme.typography.bodySmall, color = Color.secondaryText)
            } else {
                val total = max((item.size ?: 0UL).toLong(), max(item.downloaded.toLong(), 1L))
                LinearProgressIndicator(progress = { item.downloaded.toFloat() / total }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val error = item.error
                    if (error != null) {
                        Text(FileSdkUtil.friendlyError(error), style = MaterialTheme.typography.bodySmall, color = Color.Red, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    } else {
                        Text(progressInfo(vm, item), style = MaterialTheme.typography.bodySmall, color = Color.secondaryText, modifier = Modifier.weight(1f))
                    }
                    Text(stateLabel(item.state), style = MaterialTheme.typography.bodySmall, color = Color.secondaryText)
                    Icon(
                        if (item.state == DownloadState.WAITING || item.state == DownloadState.DOWNLOADING) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                        contentDescription = null,
                        tint = Color.secondaryText,
                        modifier = Modifier.padding(start = 4.dp).size(20.dp).clickable { vm.onStateClick(item) },
                    )
                }
            }
            HorizontalDivider(Modifier.padding(top = 4.dp))
        }
    }
}

/** 下载进度信息 */
private fun progressInfo(vm: FileDownloadViewModel, item: CacheInfo): String {
    val downloaded = item.downloaded.toLong().fileSize
    val speed = vm.speeds[item.id] ?: return downloaded
    return "$downloaded(${speed.toLong().fileSize}/S)"
}

/** 下载状态文字 */
private fun stateLabel(state: DownloadState): String {
    return when (state) {
        DownloadState.WAITING -> "等待下载"
        DownloadState.DOWNLOADING -> "下载中"
        DownloadState.PAUSED -> "已暂停"
        DownloadState.ERROR -> "下载失败"
        DownloadState.FINISH -> "下载完成"
    }
}
