package cn.dairo.dfs.page.albumgroup.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cn.dairo.dfs.page.album.viewmodel.AlbumViewModel
import cn.dairo.dfs.page.albumgroup.entity.AlbumGroup

/** 每个分组的最小宽度,屏幕较宽时一行显示多个分组 */
private val MIN_CARD_WIDTH = 340.dp

/** 分组之间的间距 */
private val SPACING = 16.dp

/**
 * 相册按年、按月分组显示的列表
 */
@Composable
fun AlbumGroupView(groups: List<AlbumGroup>, vm: AlbumViewModel) {
    BoxWithConstraints(Modifier.fillMaxSize().statusBarsPadding()) {
        val contentWidth = maxWidth - SPACING * 2
        val columnCount = maxOf(1, (contentWidth / MIN_CARD_WIDTH).toInt())
        val cardWidth = (contentWidth - SPACING * (columnCount - 1)) / columnCount
        val state = rememberLazyGridState()

        //打开时滚动到最底部,显示最新的照片
        LaunchedEffect(Unit) {
            state.scrollToItem(groups.size)
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(columnCount),
            state = state,
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(SPACING, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(SPACING),

            //底部留出空间显示切换按钮
            contentPadding = PaddingValues(start = SPACING, end = SPACING, top = SPACING, bottom = 100.dp),
        ) {
            items(groups, key = { it.title }) { group ->
                AlbumGroupCardView(group, cardWidth, vm::onGroupPhotoClick)
            }
        }
    }
}
