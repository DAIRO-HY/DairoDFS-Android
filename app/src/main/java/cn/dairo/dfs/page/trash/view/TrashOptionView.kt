package cn.dairo.dfs.page.trash.view

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Restore
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
import cn.dairo.dfs.page.trash.viewmodel.TrashViewModel

@Composable
fun TrashOptionView(vm: TrashViewModel, modifier: Modifier = Modifier) {

    //显示还原对话框
    var showRecoverAlert by remember { mutableStateOf(false) }

    //显示删除对话框
    var showDeleteAlert by remember { mutableStateOf(false) }

    Row(modifier.fillMaxWidth().navigationBarsPadding().padding(vertical = 8.dp)) {
        OptionButton("全选", Icons.Default.Checklist, onClick = vm::selectAll)
        OptionButton("还原", Icons.Default.Restore, disabled = vm.selectedCount == 0) {
            showRecoverAlert = true
        }
        OptionButton("彻底删除", Icons.Default.DeleteForever, disabled = vm.selectedCount == 0) {
            showDeleteAlert = true
        }
    }
    AppAlert(
        isPresented = showRecoverAlert,
        title = "确认还原选中的吗？",
        onDismiss = { showRecoverAlert = false },
        buttons = listOf(
            AlertButton("还原", ButtonRole.DESTRUCTIVE, vm::onRecoverClick),
            AlertButton("取消", ButtonRole.CANCEL),
        ),
    )
    AppAlert(
        isPresented = showDeleteAlert,
        title = "确认彻底删除吗？",
        message = "此操作无法撤销",
        onDismiss = { showDeleteAlert = false },
        buttons = listOf(
            AlertButton("彻底删除", ButtonRole.DESTRUCTIVE, vm::onDeleteClick),
            AlertButton("取消", ButtonRole.CANCEL),
        ),
    )
}
