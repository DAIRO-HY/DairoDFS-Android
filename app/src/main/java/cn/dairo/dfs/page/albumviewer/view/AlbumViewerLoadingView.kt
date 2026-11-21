package cn.dairo.dfs.page.albumviewer.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cn.dairo.dfs.components.liquidGlassBackground
import cn.dairo.dfs.extension.primaryText
import cn.dairo.dfs.page.albumviewer.viewmodel.AlbumViewerViewModel
import kotlinx.coroutines.delay

/** 加载超过这个时间才显示,避免网络较好时提示一闪而过,单位:毫秒 */
private const val SHOW_DELAY = 500L

/**
 * 加载大图、原图以及下载、保存时的进度提示,显示在右上角
 * 图片因为网络问题加载失败时,在同一个位置显示红色的联网失败图标
 */
@Composable
fun AlbumViewerLoadingView(vm: AlbumViewerViewModel, modifier: Modifier = Modifier) {
    var isShow by remember { mutableStateOf(false) }
    val isLoading = vm.progressTip != null
    LaunchedEffect(isLoading) {
        isShow = false
        if (isLoading) {
            delay(SHOW_DELAY)
            isShow = true
        }
    }

    //放在顶部按钮和时间的下方,避免屏幕较窄时重叠
    Box(modifier.statusBarsPadding().padding(top = 72.dp, end = 8.dp)) {
        val progressTip = vm.progressTip
        if (isShow && progressTip != null) {
            Row(
                Modifier.height(32.dp).liquidGlassBackground(RoundedCornerShape(16.dp)).padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp)
                if (progressTip.isNotEmpty()) {
                    Text(progressTip, style = MaterialTheme.typography.labelSmall, color = Color.primaryText, maxLines = 1)
                }
            }
        } else if (vm.isNetworkFail) {
            Box(Modifier.size(44.dp, 32.dp).liquidGlassBackground(RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.WifiOff, contentDescription = "网络连接失败", tint = Color.Red, modifier = Modifier.size(18.dp))
            }
        }
    }
}
