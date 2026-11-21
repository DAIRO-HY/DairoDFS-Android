package cn.dairo.dfs.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import cn.dairo.dfs.R
import cn.dairo.dfs.api.model.MineModel
import cn.dairo.dfs.util.FileSource

/**
 * 用户头像,没有头像时显示App图标
 */
@Composable
fun UserAvatar(user: MineModel?, size: Dp, modifier: Modifier = Modifier) {
    val source = user?.let { FileSource.avatar(it) }
    Box(modifier.size(size).clip(CircleShape)) {
        if (source != null) {
            DfsImage(source, Modifier.fillMaxSize())
        } else {
            Image(painterResource(R.drawable.logo), contentDescription = null, modifier = Modifier.fillMaxSize())
        }
    }
}
