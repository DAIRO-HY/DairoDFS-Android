package cn.dairo.dfs.page.albumviewer.view

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.ContentFrame
import cn.dairo.dfs.components.DfsImage
import cn.dairo.dfs.extension.thumbSource
import cn.dairo.dfs.page.albumviewer.viewmodel.AlbumViewerViewModel

/**
 * 当前显示的视频
 */
@OptIn(UnstableApi::class)
@Composable
fun AlbumViewerVideoView(vm: AlbumViewerViewModel, modifier: Modifier = Modifier) {
    Box(modifier.background(Color.Black)) {
        val player = vm.videoPlayer
        if (vm.videoIsPlayed && player != null) {//如果视频播放过
            ContentFrame(player, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
        }
        //账号关闭了生成缩略图时视频没有缩略图,这时直接显示黑色背景
        if ((!vm.videoIsPlayed || vm.videoIsLoading) && vm.fm.hasThumb) {//视频还没有开始播放时显示缩略图,否则加载期间是黑屏
            DfsImage(vm.fm.thumbSource, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
        }
    }

    //操作按钮隐藏时不需要更新播放进度
    LaunchedEffect(vm.showActionView) {
        if (vm.showActionView) {
            if (vm.videoIsPlaying) {
                vm.startVideoTimer()
            }
        } else {
            vm.stopVideoTimer()
        }
    }
}
