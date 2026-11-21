package cn.dairo.dfs.page.systemalbum.view

import android.graphics.Bitmap
import android.os.Build
import android.provider.MediaStore
import android.util.LruCache
import android.util.Size
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cn.dairo.dfs.page.album.view.TEXT_SHADOW
import cn.dairo.dfs.page.systemalbum.model.SystemAlbumItem
import cn.dairo.ui.DairoUI
import cn.dairo.ui.root.Toast
import cn.dairo.ui.util.timeFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 相册缩略图的内存缓存 */
private val thumbCache = object : LruCache<String, ImageBitmap>((Runtime.getRuntime().maxMemory() / 16).toInt()) {
    override fun sizeOf(key: String, value: ImageBitmap): Int = value.width * value.height * 4
}

/** 清空相册缩略图的内存缓存,App进入后台或内存紧张时调用 */
fun clearSystemAlbumThumbCache() {
    thumbCache.evictAll()
}

/**
 * 系统相册里的一个照片或视频
 * @param size 格子的边长
 */
@Composable
fun SystemAlbumImageView(item: SystemAlbumItem, size: Dp, modifier: Modifier = Modifier) {
    var image by remember(item.identifier) { mutableStateOf(thumbCache.get(item.identifier)) }
    LaunchedEffect(item.identifier) {
        if (image == null) {
            image = withContext(Dispatchers.IO) { loadThumbnail(item) }?.also { thumbCache.put(item.identifier, it) }
        }
    }
    Box(modifier.size(size).background(Color.Black)) {
        image?.let {
            Image(it, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
        if (item.checked) {//如果是选择状态
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF007AFF),
                modifier = Modifier.align(Alignment.TopStart).padding(8.dp).size(20.dp).background(Color.White, CircleShape),
            )
        }
        if (item.duration > 0) {//视频时长
            Text(
                item.duration.timeFormat,
                style = MaterialTheme.typography.labelSmall.copy(shadow = TEXT_SHADOW),
                color = Color.White,
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 5.dp, bottom = if (item.uploadMsg.isEmpty()) 5.dp else 24.dp),
            )
        }
        if (item.uploadMsg.isNotEmpty()) {//上传状态
            Text(
                item.uploadMsg,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(20.dp)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { Toast.show(item.uploadMsg) }
                    .padding(top = 2.dp),
            )
        }
    }
}

/** 读取系统生成的缩略图 */
private fun loadThumbnail(item: SystemAlbumItem): ImageBitmap? {
    val resolver = DairoUI.context.contentResolver
    val bitmap: Bitmap? = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            resolver.loadThumbnail(item.uri, Size(256, 256), null)
        } else if (item.isVideo) {
            @Suppress("DEPRECATION")
            MediaStore.Video.Thumbnails.getThumbnail(resolver, item.id, MediaStore.Video.Thumbnails.MINI_KIND, null)
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Thumbnails.getThumbnail(resolver, item.id, MediaStore.Images.Thumbnails.MINI_KIND, null)
        }
    }.getOrNull()
    return bitmap?.asImageBitmap()
}
