package cn.dairo.dfs.page.albumviewer.view

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cn.dairo.dfs.api.model.FileModel
import cn.dairo.dfs.components.DfsImage
import cn.dairo.dfs.extension.isVideo
import cn.dairo.dfs.extension.thumbSource
import cn.dairo.dfs.page.albumviewer.viewmodel.AlbumViewerViewModel
import kotlin.math.abs
import kotlin.math.min

/** 压暗程度,让图片和背景有明显的边界 */
private const val DIM_OPACITY = 0.25f

/**
 * 查看页的背景,图片不够显示的区域用模糊的缩略图填充,视频是黑色
 * Android 12以下不支持模糊,只用黑色
 */
@Composable
fun AlbumViewerBackdropView(vm: AlbumViewerViewModel, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().background(Color.Black).clipToBounds()) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

            //要切换到的页面的背景盖在当前页面的背景上面
            for (index in listOf(vm.currentIndex, vm.currentIndex - 1, vm.currentIndex + 1)) {
                val fm = vm.fileModels.getOrNull(index) ?: continue
                Backdrop(fm, Modifier.fillMaxSize().alpha(opacity(vm, index)))
            }
        }
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = DIM_OPACITY)))
    }
}

/** 一个文件的背景 */
@Composable
private fun Backdrop(fm: FileModel, modifier: Modifier) {
    if (fm.isVideo || !fm.hasThumb) {
        Box(modifier.background(Color.Black))
    } else {
        DfsImage(fm.thumbSource, modifier.blur(40.dp))
    }
}

/** 背景的透明度,滑动页面时当前页的背景不变,要切换到的页面的背景逐渐显示出来 */
private fun opacity(vm: AlbumViewerViewModel, index: Int): Float {
    if (index == vm.currentIndex) {
        return 1f
    }
    if (vm.screenWidth == 0f) {
        return 0f
    }

    //滑动进度,向左滑动切换到下一张时为正数
    val progress = (-(vm.currentIndex + 1) * vm.screenWidth - vm.hStackOffset) / vm.screenWidth
    val isTarget = if (index > vm.currentIndex) progress > 0 else progress < 0
    return if (isTarget) min(abs(progress), 1f) else 0f
}
