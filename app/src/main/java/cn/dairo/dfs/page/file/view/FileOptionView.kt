package cn.dairo.dfs.page.file.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
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
import cn.dairo.dfs.components.OptionButton
import cn.dairo.dfs.components.TextFieldAlert
import cn.dairo.dfs.page.file.viewmodel.FileViewModel

/**
 * 选择模式下的操作按钮
 */
@Composable
fun FileOptionView(vm: FileViewModel) {

    //显示删除对话框
    var showDeleteAlert by remember { mutableStateOf(false) }

    //显示重命名输入框
    var showRenameInput by remember { mutableStateOf(false) }

    //重命名输入内容
    var renameInput by remember { mutableStateOf("") }

    if (!vm.isSelectMode) {
        return
    }
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth()) {
            OptionButton("全选", Icons.Default.Checklist, onClick = vm::selectAll)
            OptionButton("复制", Icons.Default.ContentCopy, disabled = vm.selectedCount == 0) {
                vm.toClipboard(2)
            }
            OptionButton("剪切", Icons.Default.ContentCut, disabled = vm.selectedCount == 0) {
                vm.toClipboard(1)
            }
            OptionButton("粘贴", Icons.Default.ContentPaste, disabled = vm.clipboardType == 0, onClick = vm::onPasteClick)
        }
        Row(Modifier.fillMaxWidth()) {
            OptionButton("删除", Icons.Default.Delete, disabled = vm.selectedCount == 0) {
                showDeleteAlert = true
            }
            OptionButton("下载", Icons.Default.Download, disabled = vm.selectedCount == 0, onClick = vm::onDownloadClick)
            OptionButton("刷新", Icons.Default.Refresh, onClick = vm::reload)
            OptionButton("重命名", Icons.Default.Edit, disabled = vm.selectedCount != 1) {

                //当前选中文件名
                renameInput = vm.selectedName
                showRenameInput = true
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
    TextFieldAlert(
        isPresented = showRenameInput,
        title = "重命名",
        text = renameInput,
        onTextChange = { renameInput = it },
        placeholder = "文件名",
        onDismiss = { showRenameInput = false },
        buttons = listOf(
            AlertButton("修改", ButtonRole.DESTRUCTIVE) {
                vm.onRenameClick(renameInput)
            },
            AlertButton("取消", ButtonRole.CANCEL),
        ),
    )
}
