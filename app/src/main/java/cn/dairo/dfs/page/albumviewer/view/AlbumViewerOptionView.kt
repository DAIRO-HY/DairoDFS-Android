package cn.dairo.dfs.page.albumviewer.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddToPhotos
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cn.dairo.dfs.components.ActionSheet
import cn.dairo.dfs.components.AlertButton
import cn.dairo.dfs.components.AppAlert
import cn.dairo.dfs.components.ButtonRole
import cn.dairo.dfs.components.OptionButton
import cn.dairo.dfs.page.albumviewer.viewmodel.AlbumViewerViewModel
import cn.dairo.dfs.store.SettingStore
import cn.dairo.ui.util.timeFormat
import kotlin.math.max

/**
 * 底部的操作按钮,视频时上面还有进度条
 */
@Composable
fun AlbumViewerOptionView(vm: AlbumViewerViewModel, modifier: Modifier = Modifier) {
    var showDeleteAlert by remember { mutableStateOf(false) }

    /** 视频清晰度选择框是否显示 */
    var showVideoResolutionMenu by remember { mutableStateOf(false) }

    Column(modifier.fillMaxWidth().navigationBarsPadding()) {
        if (vm.isVideo) {//如果这是一个视频的话

            //视频背景是黑色的,浅色模式下也使用深色
            Column(
                Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
                    .background(Color(0xCC1C1C1E), RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                val range = 0f..max(vm.videoDuration, 1.0).toFloat()
                val sliderState = remember(range) { SliderState(0f, 0, range) }

                //外部的播放进度同步给进度条
                sliderState.value = vm.videoCurrentTime.toFloat().coerceIn(range)
                Slider(
                    state = sliderState,
                    onValueChange = {
                        vm.videoSliderDragging = true
                        vm.videoCurrentTime = it.toDouble()
                    },
                    onValueChangeFinished = {
                        vm.videoSliderDragging = false
                        vm.videoPlayer?.seekTo(vm.videoCurrentTime.toLong())
                    },
                    colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White, inactiveTrackColor = Color.White.copy(alpha = 0.3f)),
                )
                Row {
                    Text(vm.videoCurrentTime.timeFormat, style = MaterialTheme.typography.bodySmall, color = Color.White)
                    Spacer(Modifier.weight(1f))
                    Text(vm.videoDuration.timeFormat, style = MaterialTheme.typography.bodySmall, color = Color.White)
                }
            }
        }
        Row(Modifier.fillMaxWidth()) {
            OptionButton("删除", Icons.Default.Delete) {
                showDeleteAlert = true
            }

            //下载进度显示在加载提示里,下载期间不能再次点击
            OptionButton("下载", Icons.Default.Download, disabled = vm.isDownloading) {
                vm.onDownloadOnlyClick()
            }
            OptionButton("保存", Icons.Default.AddToPhotos, disabled = vm.isDownloading) {
                vm.onDownloadOnlyClick(true)
            }
            if (vm.isImage) {//图片时
                OptionButton("原图", Icons.Default.Photo, disabled = vm.isShowDownloaded, onClick = vm::onLoadOriginalClick)
            } else {//视频时
                OptionButton(vm.videoQualityLabel, Icons.Default.HighQuality, disabled = vm.isShowDownloaded) {
                    showVideoResolutionMenu = true
                }
            }
            OptionButton("属性", Icons.Default.Info, onClick = vm::onInfoClick)
        }
        Spacer(Modifier.padding(bottom = 32.dp))
    }
    AppAlert(
        isPresented = showDeleteAlert,
        title = "确认删除吗？",
        message = "此操作无法撤销",
        onDismiss = { showDeleteAlert = false },
        buttons = listOf(
            AlertButton("删除", ButtonRole.DESTRUCTIVE, vm::onDeleteClick),
            AlertButton("取消", ButtonRole.CANCEL),
        ),
    )
    ActionSheet(
        isPresented = showVideoResolutionMenu,
        title = "视频清晰度",
        onDismiss = { showVideoResolutionMenu = false },
        buttons = listOf(
            SettingStore.VIDEO_QUALITY_AUTO to "自动",
            SettingStore.VIDEO_QUALITY_ORIGINAL to "原画",
            SettingStore.VIDEO_QUALITY_SMOOTH to "流畅",
        ).map { (quality, label) ->
            AlertButton((if (SettingStore.videoQuality == quality) "✓" else "") + label) {
                vm.onVideoSelectResolutionClick(quality)
            }
        } + AlertButton("取消", ButtonRole.CANCEL),
    )
}
