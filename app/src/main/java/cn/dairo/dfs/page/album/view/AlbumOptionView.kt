package cn.dairo.dfs.page.album.view

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cn.dairo.dfs.components.AlertButton
import cn.dairo.dfs.components.AppAlert
import cn.dairo.dfs.components.ButtonRole
import cn.dairo.dfs.components.DfsImageLoader
import cn.dairo.dfs.components.OptionButton
import cn.dairo.dfs.components.relaunch
import cn.dairo.dfs.page.album.viewmodel.AlbumViewModel
import cn.dairo.dfs.page.file.FilePage

/**
 * 选择模式下的操作按钮
 * @param onAddClick 添加按钮点击事件
 */
@Composable
fun AlbumOptionView(vm: AlbumViewModel, onAddClick: () -> Unit, modifier: Modifier = Modifier) {
    var showDeleteAlert by remember { mutableStateOf(false) }
    if (!vm.isSelectMode) {
        return
    }
    Row(modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 30.dp)) {
        OptionButton("刷新", Icons.Default.Refresh) {
            vm.loadData()
            DfsImageLoader.retryFailed()
        }
        OptionButton("删除", Icons.Default.Delete, disabled = vm.selectedCount == 0) {
            showDeleteAlert = true
        }
        OptionButton("添加", Icons.Default.AddBox, onClick = onAddClick)
        OptionButton("下载", Icons.Default.Download, disabled = vm.selectedCount == 0, onClick = vm::onDownloadClick)
        OptionButton("退出", Icons.AutoMirrored.Filled.Logout) {
            relaunch { FilePage() }
        }
    }
    AppAlert(
        isPresented = showDeleteAlert,
        title = "确认删除选中的${vm.selectedCount}个文件吗？",
        message = "此操作无法撤销",
        onDismiss = { showDeleteAlert = false },
        buttons = listOf(
            AlertButton("删除", ButtonRole.DESTRUCTIVE, vm::onDeleteClick),
            AlertButton("取消", ButtonRole.CANCEL),
        ),
    )
}
