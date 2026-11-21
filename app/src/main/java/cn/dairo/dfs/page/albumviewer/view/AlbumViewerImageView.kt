package cn.dairo.dfs.page.albumviewer.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material3.MaterialTheme
import cn.dairo.dfs.extension.isDlive
import cn.dairo.dfs.page.album.view.TEXT_SHADOW
import cn.dairo.dfs.page.albumviewer.viewmodel.AlbumViewerViewModel

/**
 * 当前显示的图片
 */
@Composable
fun AlbumViewerImageView(vm: AlbumViewerViewModel, modifier: Modifier = Modifier) {
    Box(modifier.clipToBounds(), contentAlignment = Alignment.Center) {
        vm.uiImage?.let { image ->
            Image(
                image,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().graphicsLayer {
                    scaleX = vm.zoomAmount
                    scaleY = vm.zoomAmount
                    translationX = vm.currentOffsetPosition.x
                    translationY = vm.currentOffsetPosition.y
                },
            )
        }
        if (vm.fm.isDlive) {
            AlbumViewerDliveView(vm.dliveVm, Modifier.fillMaxSize())
        }
        if (vm.errorMsg.isNotEmpty()) {
            Text(vm.errorMsg, color = Color.White, style = MaterialTheme.typography.bodyMedium.copy(shadow = TEXT_SHADOW))
        }
    }

    //视图被注销时,取消下载
    DisposableEffect(Unit) {
        onDispose { vm.cancelFetch() }
    }
}
