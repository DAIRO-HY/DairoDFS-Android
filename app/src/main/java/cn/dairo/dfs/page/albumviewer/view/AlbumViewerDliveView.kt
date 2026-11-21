package cn.dairo.dfs.page.albumviewer.view

import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.ContentFrame
import cn.dairo.dfs.page.album.view.TEXT_SHADOW
import cn.dairo.dfs.page.albumviewer.viewmodel.AlbumViewerDliveViewModel

/**
 * 实况照片的视频,播放结束之后显示照片
 */
@OptIn(UnstableApi::class)
@Composable
fun AlbumViewerDliveView(vm: AlbumViewerDliveViewModel, modifier: Modifier = Modifier) {
    if (!vm.isShow) {
        return
    }
    Box(modifier, contentAlignment = Alignment.Center) {
        val player = vm.dlivePlayer
        if (player != null) {
            ContentFrame(player, modifier, contentScale = ContentScale.Fit)
        } else {
            Text(vm.progress, color = Color.White, style = MaterialTheme.typography.bodyMedium.copy(shadow = TEXT_SHADOW))
        }
    }
}
