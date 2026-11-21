package cn.dairo.dfs.page.albumgroup.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cn.dairo.dfs.components.liquidGlassBackground
import cn.dairo.dfs.extension.isDarkTheme
import cn.dairo.dfs.extension.primaryText
import cn.dairo.dfs.page.album.viewmodel.AlbumViewModel
import cn.dairo.dfs.page.albumgroup.entity.AlbumViewMode

/**
 * 底部切换相册显示方式的按钮
 */
@Composable
fun AlbumModeBarView(vm: AlbumViewModel, modifier: Modifier = Modifier) {
    Row(modifier.navigationBarsPadding().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ModeButton(vm, "按年", AlbumViewMode.YEAR)
        ModeButton(vm, "按月", AlbumViewMode.MONTH)
        ModeButton(vm, "全部", AlbumViewMode.ALL)
    }
}

@Composable
private fun ModeButton(vm: AlbumViewModel, label: String, mode: AlbumViewMode) {
    val isSelected = vm.viewMode == mode
    val shape = RoundedCornerShape(20.dp)

    //选中的按钮加一点颜色
    val selectedColor = if (isDarkTheme) Color(0xFF48484A) else Color(0xFFDCDCE0)
    Box(
        Modifier
            .size(72.dp, 40.dp)
            .liquidGlassBackground(shape, if (isSelected) selectedColor else null)
            .clickable { vm.viewMode = mode },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = Color.primaryText,
        )
    }
}
