package cn.dairo.dfs.page.albumviewer.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cn.dairo.dfs.extension.isDlive
import cn.dairo.dfs.page.albumviewer.viewmodel.AlbumViewerViewModel

/** 标签的形状 */
private val TAG_SHAPE = RoundedCornerShape(50)

/**
 * 左上角的标签,实况照片还有声音开关
 */
@Composable
fun AlbumViewerTagView(vm: AlbumViewerViewModel, modifier: Modifier = Modifier) {
    Row(
        modifier.statusBarsPadding().padding(start = 16.dp, top = 72.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        vm.tags.forEach {
            Text(
                it,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                modifier = Modifier.background(Color.Black.copy(alpha = 0.6f), TAG_SHAPE).padding(horizontal = 6.dp, vertical = 4.dp),
            )
        }
        if (vm.fm.isDlive) {//实况照片的声音开关,样式和标签一样
            val dliveVm = vm.dliveVm
            Icon(
                if (dliveVm.isSoundOn) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                contentDescription = if (dliveVm.isSoundOn) "关闭实况声音" else "打开实况声音",
                tint = Color.White,
                modifier = Modifier
                    .clip(TAG_SHAPE)
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable(onClick = dliveVm::onSoundClick)
                    .padding(4.dp)
                    .size(16.dp),
            )
        }
    }
}
