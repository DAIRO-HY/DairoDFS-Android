package cn.dairo.dfs.page.trash

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
import cn.dairo.dfs.page.trash.view.TrashItemView
import cn.dairo.dfs.page.trash.view.TrashOptionView
import cn.dairo.dfs.page.trash.viewmodel.TrashViewModel
import cn.dairo.dfs.util.rememberStateObject
import cn.dairo.ui.setting.settingRowClickable

@Composable
fun TrashPage() {
    val vm = rememberStateObject { TrashViewModel() }
    NavigationPage(title = "回收站") {
        Box(Modifier.fillMaxSize()) {

            //操作按钮浮在列表上面,列表底部留出按钮的位置
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 100.dp)) {
                items(vm.entities, key = { it.fm.id }) { item ->
                    TrashItemView(item, Modifier.settingRowClickable { vm.onItemClick(item) })
                }
            }
            TrashOptionView(vm, Modifier.align(Alignment.BottomCenter))
        }
    }
}
