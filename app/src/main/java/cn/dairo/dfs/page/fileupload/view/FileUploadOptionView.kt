package cn.dairo.dfs.page.fileupload.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cn.dairo.dfs.components.AlertButton
import cn.dairo.dfs.components.AppAlert
import cn.dairo.dfs.components.ButtonRole
import cn.dairo.dfs.components.OptionButton
import cn.dairo.dfs.components.liquidGlassBackground
import cn.dairo.dfs.extension.primaryText
import cn.dairo.dfs.page.fileupload.FileUploadViewModel
import cn.dairo.dfs.page.fileuploadset.FileUploadSetPage
import cn.dairo.ui.setting.LocalSettingNavigator

@Composable
fun FileUploadOptionView(vm: FileUploadViewModel, modifier: Modifier = Modifier) {
    val navigator = LocalSettingNavigator.current
    var showDeleteAlert by remember { mutableStateOf(false) }
    Column(modifier.fillMaxWidth().navigationBarsPadding().padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "已选择:${vm.checked.size}",
            style = MaterialTheme.typography.bodySmall,
            color = Color.primaryText,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .liquidGlassBackground(RoundedCornerShape(50))
                .padding(horizontal = 12.dp, vertical = 6.dp),
        )
        Row(Modifier.fillMaxWidth()) {
            OptionButton("全选", Icons.Default.Checklist, onClick = vm::onCheckAllClick)
            OptionButton("删除", Icons.Default.Delete, disabled = vm.checked.isEmpty()) {
                showDeleteAlert = true
            }
            OptionButton("全暂停", Icons.Default.PauseCircle, onClick = vm::onPauseAllClick)
            OptionButton("全开始", Icons.Default.PlayCircle, onClick = vm::onStartAllClick)
            OptionButton("设置", Icons.Default.Settings) {
                navigator?.push { FileUploadSetPage() }
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
