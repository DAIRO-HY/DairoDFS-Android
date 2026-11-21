package cn.dairo.dfs.page.systemalbum.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cn.dairo.dfs.components.OptionButton
import cn.dairo.dfs.components.liquidGlassBackground
import cn.dairo.dfs.extension.primaryText
import cn.dairo.dfs.page.systemalbum.viewmodel.SystemAlbumViewModel
import cn.dairo.dfs.page.systemalbum.viewmodel.UploadMode

/**
 * 底部的上传进度和操作按钮
 * @param onDeleteClick 删除已上传按钮点击事件
 */
@Composable
fun SystemAlbumOptionView(vm: SystemAlbumViewModel, onDeleteClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(50)
    Column(modifier.fillMaxWidth().navigationBarsPadding().padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        vm.uploadCountMsg?.let { msg ->//上传数量通知
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(msg, style = MaterialTheme.typography.bodySmall, color = Color.primaryText, modifier = Modifier.liquidGlassBackground(shape).padding(horizontal = 12.dp, vertical = 6.dp))
                Spacer(Modifier.weight(1f))
                if (vm.isUploading) {//正在上传中,显示取消按钮
                    Text(
                        "取消",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.primaryText,
                        modifier = Modifier.liquidGlassBackground(shape).clickable(onClick = vm::onCancelClick).padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }
        }
        Row(Modifier.fillMaxWidth()) {
            OptionButton("全选", Icons.Default.Checklist, onClick = vm::onCheckAllClick)
            OptionButton("上传", Icons.Default.Upload, disabled = vm.checkedCount == 0 || vm.isUploading) {
                vm.onUploadClick(false)
            }
            if (vm.uploadMode == UploadMode.ALBUM) {//只有相册同步模式才允许检查
                OptionButton("同步检查", Icons.Default.CloudSync) {
                    vm.onUploadClick(true)
                }
            }
            OptionButton("删除已上传", Icons.Default.Delete, disabled = vm.isUploading, onClick = onDeleteClick)
        }
    }
}
