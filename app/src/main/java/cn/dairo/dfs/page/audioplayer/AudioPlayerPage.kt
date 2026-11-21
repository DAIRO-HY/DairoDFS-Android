package cn.dairo.dfs.page.audioplayer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cn.dairo.dfs.api.model.FileModel
import cn.dairo.dfs.extension.primaryText
import cn.dairo.dfs.extension.secondaryText
import cn.dairo.dfs.util.rememberStateObject
import cn.dairo.ui.util.timeFormat
import kotlin.math.max
import kotlin.math.min

/**
 * 音频播放页面,App进入后台后继续播放,关闭页面时停止播放
 * @param onClose 关闭按钮点击事件
 */
@Composable
fun AudioPlayerPage(fm: FileModel, onClose: () -> Unit) {
    val vm = rememberStateObject { AudioPlayerViewModel(fm) }
    Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Row(Modifier.fillMaxWidth()) {
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onClose) {
                Text("关闭")
            }
        }
        Spacer(Modifier.weight(1f))
        Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color.secondaryText, modifier = Modifier.size(96.dp))
        Text(vm.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = Color.primaryText, textAlign = TextAlign.Center)
        vm.errorMsg?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = Color.Red)
        }
        Column(Modifier.fillMaxWidth()) {
            val range = 0f..max(vm.duration, 1.0).toFloat()
            val sliderState = remember(range) { SliderState(0f, 0, range) }

            //外部的播放进度同步给进度条
            sliderState.value = min(vm.currentTime, vm.duration).toFloat().coerceIn(range)
            Slider(
                state = sliderState,
                onValueChange = {
                    vm.isDragging = true
                    vm.currentTime = it.toDouble()
                },
                onValueChangeFinished = {
                    vm.isDragging = false
                    vm.seek(vm.currentTime)
                },
                enabled = vm.duration > 0,
            )
            Row {
                Text(format(vm.currentTime), style = MaterialTheme.typography.bodySmall, color = Color.secondaryText)
                Spacer(Modifier.weight(1f))
                Text(format(vm.duration), style = MaterialTheme.typography.bodySmall, color = Color.secondaryText)
            }
        }
        Icon(
            if (vm.isPlaying) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
            contentDescription = if (vm.isPlaying) "暂停" else "播放",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp).clip(CircleShape).clickable(enabled = vm.errorMsg == null, onClick = vm::onPlayClick),
        )
        Spacer(Modifier.weight(1f))
    }
}

/** 格式化播放时间 */
private fun format(seconds: Double): String {
    return ((if (seconds.isFinite()) seconds else 0.0) * 1000).timeFormat
}
