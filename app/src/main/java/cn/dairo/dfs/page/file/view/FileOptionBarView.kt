package cn.dairo.dfs.page.file.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cn.dairo.dfs.components.liquidGlassBackground
import cn.dairo.dfs.extension.primaryText
import cn.dairo.dfs.page.file.viewmodel.FileViewModel

/**
 * 浮在列表上面的顶部按钮:返回上一级、回到根目录、当前路径、选择模式
 */
@Composable
fun FileOptionBarView(vm: FileViewModel, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 10.dp).padding(top = 4.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OptionBtn(Icons.AutoMirrored.Filled.ArrowBack, "返回上一级", vm::onBackClick)
        OptionBtn(Icons.Default.Home, "根目录") {//加载首页数据
            vm.loadSubFile("")
        }
        //占满中间剩下的宽度,右边的按钮始终靠右;路径短时靠左显示,太长时省略前面的部分
        //不能和路径各用一个weight,否则剩下的宽度会被平分,路径短时右边的按钮会偏到中间
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (vm.currentFolder.isNotEmpty()) {//页面刚打开还没有路径
                Box(
                    Modifier.height(36.dp).liquidGlassBackground(RoundedCornerShape(18.dp)).padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {

                    //路径太长时优先显示当前文件夹
                    Text(
                        vm.currentFolder,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.primaryText,
                        maxLines = 1,
                        overflow = TextOverflow.StartEllipsis,
                    )
                }
            }
        }
        OptionBtn(if (vm.isSelectMode) Icons.Default.Close else Icons.Default.MoreHoriz, if (vm.isSelectMode) "取消选择" else "选择") {
            vm.isShowAddView = false
            vm.isSelectMode = !vm.isSelectMode
            vm.clearSelected()
        }
    }
}

/** 功能按钮 */
@Composable
private fun OptionBtn(icon: ImageVector, label: String, action: () -> Unit) {
    Box(
        Modifier.size(36.dp).liquidGlassBackground(CircleShape).clickable(onClick = action),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = label, tint = Color.primaryText, modifier = Modifier.size(20.dp))
    }
}
