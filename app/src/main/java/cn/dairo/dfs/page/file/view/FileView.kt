package cn.dairo.dfs.page.file.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cn.dairo.dfs.extension.secondaryText
import cn.dairo.dfs.page.file.viewmodel.FileViewModel
import cn.dairo.ui.setting.settingRowClickable

/**
 * 文件列表
 * @param contentPadding 列表上下留出浮动按钮的位置
 */
@Composable
fun FileView(vm: FileViewModel, contentPadding: PaddingValues) {
    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = contentPadding) {
            items(vm.entities, key = { it.fm.id }) { item ->
                FileListItemView(
                    item,
                    isSelectMode = vm.isSelectMode,
                    modifier = Modifier
                        .settingRowClickable { vm.onFileClick(item) }

                        //剪切中的文件
                        .alpha(if (vm.isMoveFlag(item.fm.id)) 0.5f else 1f),
                )
            }
        }
        if (vm.isLoading) {
            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CircularProgressIndicator()
                Text("加载中", color = Color.secondaryText)
            }
        }
    }
}
