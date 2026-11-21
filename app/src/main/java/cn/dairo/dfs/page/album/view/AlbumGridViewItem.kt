package cn.dairo.dfs.page.album.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cn.dairo.dfs.components.DfsImage
import cn.dairo.dfs.components.ImagePlaceholder
import cn.dairo.dfs.extension.placeholderIcon
import cn.dairo.dfs.extension.thumbOnlineSource
import cn.dairo.dfs.extension.thumbSource
import cn.dairo.dfs.page.album.entity.AlbumEntity
import cn.dairo.ui.util.timeFormat
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background

/** 图片上文字的阴影,浅色图片上也能看清 */
val TEXT_SHADOW = Shadow(Color.Black, Offset(2f, 2f), 6f)

/**
 * 相册里的一张照片
 * @param size 格子的边长
 */
@Composable
fun AlbumGridViewItem(entity: AlbumEntity, size: Dp, isSelectMode: Boolean, modifier: Modifier = Modifier) {
    val fm = entity.fm
    Box(modifier.size(size)) {
        if (fm.hasThumb) {//如果有缩略图

            //按实际像素请求在线生成的图片,生成期间先显示缩略图。查看页面用的也是这张缩略图,打开图片时不用走网络就能先显示
            val px = with(LocalDensity.current) { size.toPx() }
            DfsImage(fm.thumbOnlineSource(px, px), Modifier.fillMaxSize(), fallback = fm.thumbSource, placeholder = fm.placeholderIcon)
        } else {//服务器还没有生成缩略图
            ImagePlaceholder(fm.placeholderIcon, Modifier.fillMaxSize())
        }
        val duration = fm.duration
        if (duration != null && duration > 0) {//如果有视频时间
            Text(
                duration.timeFormat,
                style = MaterialTheme.typography.bodyMedium.copy(shadow = TEXT_SHADOW),
                color = Color.White,
                modifier = Modifier.align(Alignment.BottomEnd).padding(5.dp),
            )
        }
        if (isSelectMode && entity.isSelected) {//当前为选中状态
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF007AFF),
                modifier = Modifier.align(Alignment.BottomEnd).padding(5.dp).size(24.dp).background(Color.White, CircleShape),
            )
        }
    }
}
