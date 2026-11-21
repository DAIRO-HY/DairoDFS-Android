package cn.dairo.dfs.page.album.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cn.dairo.dfs.components.liquidGlassBackground
import cn.dairo.dfs.extension.primaryText
import cn.dairo.dfs.page.album.viewmodel.AlbumViewModel

/**
 * 右上角的选择模式按钮
 */
@Composable
fun AlbumOptionBarView(vm: AlbumViewModel, modifier: Modifier = Modifier) {
    Box(
        modifier
            .statusBarsPadding()
            .padding(16.dp)
            .size(36.dp)
            .liquidGlassBackground(CircleShape)
            .clickable {
                vm.isSelectMode = !vm.isSelectMode
                vm.clearSelected()
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            if (vm.isSelectMode) Icons.Default.Close else Icons.Default.MoreHoriz,
            contentDescription = if (vm.isSelectMode) "取消选择" else "选择",
            tint = Color.primaryText,
            modifier = Modifier.size(20.dp),
        )
    }
}
