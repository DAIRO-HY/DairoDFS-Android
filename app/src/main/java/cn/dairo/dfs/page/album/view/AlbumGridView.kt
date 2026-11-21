package cn.dairo.dfs.page.album.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cn.dairo.dfs.extension.primaryText
import cn.dairo.dfs.extension.secondaryText
import cn.dairo.dfs.page.album.viewmodel.AlbumViewModel
import cn.dairo.ui.setting.settingRowClickable

/** 表格间距 */
private val SPACING = 2.dp

/** 根据可用宽度计算列数,手机上是3列,屏幕越宽列数越多 */
fun albumColumnCount(width: Dp): Int {
    return maxOf(3, (width / 150.dp).toInt())
}

/**
 * 相册的全部照片
 * @param onAddClick 没有照片时,添加按钮点击事件
 */
@Composable
fun AlbumGridView(vm: AlbumViewModel, onAddClick: () -> Unit) {
    if (vm.isLoading) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            CircularProgressIndicator()
            Text("加载中", color = Color.secondaryText, modifier = Modifier.padding(top = 8.dp))
        }
        return
    }
    if (vm.entityList.isEmpty()) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Column(Modifier.settingRowClickable(onClick = onAddClick).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.AddCircle, contentDescription = null, tint = Color.primaryText, modifier = Modifier.size(36.dp))
                Text("添加", color = Color.primaryText)
            }
        }
        return
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val columnNum = albumColumnCount(maxWidth)

        //每列宽度
        val columnWidth = (maxWidth - SPACING * (columnNum - 1)) / columnNum
        val state = rememberLazyGridState()
        val viewportHeight = constraints.maxHeight
        LaunchedEffect(Unit) {
            val scrollToId = vm.scrollToId
            vm.scrollToId = null
            val index = scrollToId?.let { id -> vm.entityList.indexOfFirst { it.fm.id == id } } ?: -1
            if (index >= 0) {//从分组点击照片进来时,把这张照片定位到屏幕中间
                state.scrollToItem(index, -viewportHeight / 2)
            } else {//打开时滚动到最底部,显示最新的照片
                state.scrollToItem(vm.entityList.size)
            }
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(columnNum),
            state = state,
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(SPACING),
            verticalArrangement = Arrangement.spacedBy(SPACING),

            //让底部有一定空间显示操作按钮
            contentPadding = PaddingValues(bottom = 100.dp),
        ) {
            items(vm.entityList, key = { it.fm.id }) { item ->
                AlbumGridViewItem(item, columnWidth, vm.isSelectMode, Modifier.settingRowClickable { vm.onItemClick(item) })
            }
        }
    }
}
