package cn.dairo.dfs.page.albumviewer.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cn.dairo.dfs.components.liquidGlassBackground
import cn.dairo.dfs.extension.primaryText
import cn.dairo.dfs.page.albumviewer.viewmodel.AlbumViewerViewModel

/**
 * 顶部的返回按钮和文件时间
 */
@Composable
fun AlbumViewerTopBarView(vm: AlbumViewerViewModel, onClose: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().statusBarsPadding().padding(top = 16.dp, start = 8.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(40.dp).liquidGlassBackground(CircleShape).clickable(onClick = onClose),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = Color.primaryText)
        }
        Spacer(Modifier.weight(1f))
        vm.fileTime?.let {
            Box(Modifier.height(40.dp).liquidGlassBackground(RoundedCornerShape(20.dp)).padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = Color.primaryText)
            }
        }
        Spacer(Modifier.weight(1f))

        //和返回按钮同宽,使时间水平居中
        Spacer(Modifier.size(40.dp))
    }
}
