package cn.dairo.dfs.page.albumviewer

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import cn.dairo.dfs.components.DfsImage
import cn.dairo.dfs.extension.canThumbOnline
import cn.dairo.dfs.extension.isDarkTheme
import cn.dairo.dfs.extension.screenImageSource
import cn.dairo.dfs.extension.thumbSource
import cn.dairo.dfs.page.albumviewer.view.AlbumViewerBackdropView
import cn.dairo.dfs.page.albumviewer.view.AlbumViewerImageView
import cn.dairo.dfs.page.albumviewer.view.AlbumViewerInfoView
import cn.dairo.dfs.page.albumviewer.view.AlbumViewerLoadingView
import cn.dairo.dfs.page.albumviewer.view.AlbumViewerOptionView
import cn.dairo.dfs.page.albumviewer.view.AlbumViewerTagView
import cn.dairo.dfs.page.albumviewer.view.AlbumViewerTopBarView
import cn.dairo.dfs.page.albumviewer.view.AlbumViewerTouchView
import cn.dairo.dfs.page.albumviewer.view.AlbumViewerVideoView
import cn.dairo.dfs.page.albumviewer.viewmodel.AlbumViewerViewModel
import cn.dairo.dfs.util.OnFileSdkEvent
import kotlin.math.abs

/**
 * 查看图片和视频的页面,盖在整个屏幕上
 * @param onClose 关闭页面
 */
@Composable
fun AlbumViewerPage(vm: AlbumViewerViewModel, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    OnFileSdkEvent(vm::onFileSdkEvent)
    DisposableEffect(vm) {
        vm.onEmpty = onClose

        //离开时停止播放
        onDispose { vm.onCleared() }
    }
    StatusBarLight()
    BoxWithConstraints(Modifier.fillMaxSize().background(Color.Black)) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()
        val density = LocalDensity.current.density
        LaunchedEffect(width, height) {
            if (vm.screenWidth == 0f) {
                vm.onAppear(width, height, density)
            } else {
                vm.onResize(width, height)
            }
        }
        if (vm.screenWidth == 0f) {
            return@BoxWithConstraints
        }
        AlbumViewerBackdropView(vm)

        //为了实现懒加载,这里始终只显示上一张,本张,下一张共3张图片
        for (index in vm.currentIndex - 1..vm.currentIndex + 1) {
            val fm = vm.fileModels.getOrNull(index) ?: continue
            val pageModifier = Modifier.fillMaxSize().graphicsLayer { translationX = (index + 1) * vm.screenWidth + vm.hStackOffset }
            when {
                index != vm.currentIndex -> when {
                    fm.hasThumb -> DfsImage(fm.thumbSource, pageModifier, contentScale = ContentScale.Fit)

                    //没有缩略图的普通图片,显示适合屏幕的在线图片,切换过去之后查看页也用这张
                    fm.canThumbOnline -> DfsImage(fm.screenImageSource, pageModifier, contentScale = ContentScale.Fit)
                }
                vm.isVideo -> AlbumViewerVideoView(vm, pageModifier)
                else -> AlbumViewerImageView(vm, pageModifier)
            }
        }

        //避免事件穿透,专门用一个视图来操作屏幕
        AlbumViewerTouchView(vm, Modifier.fillMaxSize())
        if (vm.showActionView) {//显示操作视图
            AlbumViewerOptionView(vm, Modifier.align(Alignment.BottomCenter))
            AlbumViewerTopBarView(vm, onClose, Modifier.align(Alignment.TopCenter))
            AlbumViewerTagView(vm, Modifier.align(Alignment.TopStart))
        }

        //操作视图隐藏时也要显示
        AlbumViewerLoadingView(vm, Modifier.align(Alignment.TopEnd))
        if (vm.isVideo) {
            VideoControl(vm, Modifier.align(Alignment.Center))
            VideoTip(vm)
        }
    }
    AlbumViewerInfoView(vm)
}

/** 页面背景是黑色的,状态栏使用浅色图标,关闭页面时恢复 */
@Composable
private fun StatusBarLight() {
    val view = LocalView.current
    val isDark = isDarkTheme
    DisposableEffect(view) {
        val window = (view.context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        controller?.isAppearanceLightStatusBars = false
        onDispose {
            controller?.isAppearanceLightStatusBars = !isDark
        }
    }
}

/** 视频的加载中、播放、暂停按钮 */
@Composable
private fun VideoControl(vm: AlbumViewerViewModel, modifier: Modifier) {
    when {
        vm.videoIsLoading -> CircularProgressIndicator(modifier.size(48.dp), color = Color.White)//视频加载中,不能点击播放
        !vm.videoIsPlaying -> ControlButton(Icons.Default.PlayArrow, "播放", modifier, vm::onVideoPlayClick)//视频为暂停状态时,始终显示播放按钮
        vm.showActionView -> ControlButton(Icons.Default.Pause, "暂停", modifier, vm::onVideoPauseClick)
    }
}

@Composable
private fun ControlButton(icon: ImageVector, label: String, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.size(64.dp).background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(32.dp)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(40.dp))
    }
}

/** 视频手势操作的提示 */
@Composable
private fun VideoTip(vm: AlbumViewerViewModel) {
    Box(Modifier.fillMaxSize()) {
        if (vm.videoIsSpeedUp) {
            TipView("2倍速播放", Icons.Default.FastForward, Modifier.align(Alignment.TopCenter).padding(top = 110.dp))
        }
        vm.videoSeekTip?.let { seconds ->
            TipView(
                "${if (seconds < 0) "快退" else "快进"}${abs(seconds).toInt()}秒",
                if (seconds < 0) Icons.Default.FastRewind else Icons.Default.FastForward,
                Modifier.align(if (seconds < 0) Alignment.CenterStart else Alignment.CenterEnd).padding(horizontal = 40.dp),
            )
        }
    }
}

@Composable
private fun TipView(label: String, icon: ImageVector, modifier: Modifier) {
    Row(
        modifier.background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(20.dp)).padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Color.White)
    }
}
