package cn.dairo.dfs.page.fileupload

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cn.dairo.dfs.components.NavigationPage
import cn.dairo.dfs.page.fileupload.view.FileUploadItemView
import cn.dairo.dfs.page.fileupload.view.FileUploadOptionView
import cn.dairo.dfs.util.OnFileSdkEvent
import cn.dairo.dfs.util.rememberStateObject

@Composable
fun FileUploadPage() {
    val vm = rememberStateObject { FileUploadViewModel() }
    OnFileSdkEvent(vm::onFileSdkEvent)
    NavigationPage(title = "上传管理") {
        Box(Modifier.fillMaxSize()) {

            //操作按钮浮在列表上面,列表底部留出按钮的位置
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 140.dp)) {
                items(vm.list, key = { it.id }) { item ->
                    FileUploadItemView(vm, item)
                }
            }
            FileUploadOptionView(vm, Modifier.align(Alignment.BottomCenter))
        }
    }
}
