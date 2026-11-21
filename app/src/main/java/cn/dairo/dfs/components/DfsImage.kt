package cn.dairo.dfs.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.media.ExifInterface
import android.os.Build
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import cn.dairo.dfs.extension.secondaryText
import cn.dairo.dfs.util.FileSdkUtil
import cn.dairo.dfs.util.FileSource
import cn.dairo.file.sdk.LocalSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.nio.ByteBuffer
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * 通过SDK加载DFS图片的视图,尺寸由调用方通过modifier指定
 * 页面关闭时连接断开,SDK会自动取消下载
 * @param fallback 图片加载中或加载失败时显示的图片
 * @param placeholder 图片显示出来之前的占位图标,不指定时加载中显示空白
 */
@Composable
fun DfsImage(
    source: FileSource,
    modifier: Modifier = Modifier,
    fallback: FileSource? = null,
    placeholder: ImageVector? = null,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val key = source.cacheKey
    var image by remember(key) { mutableStateOf(DfsImageLoader.get(key)) }

    //图片是否加载失败
    var isFail by remember(key) { mutableStateOf(false) }

    //重试次数,改变时重新加载
    var retryCount by remember(key) { mutableIntStateOf(0) }

    //重新加载所有加载失败的图片,正在加载的不用重试,否则会取消之后重新排队
    val retryVersion = DfsImageLoader.retryVersion
    LaunchedEffect(retryVersion) {
        if (isFail) {
            retryCount += 1
        }
    }
    LaunchedEffect(key, retryCount) {
        if (image != null) {
            return@LaunchedEffect
        }
        isFail = false
        val loaded = DfsImageLoader.load(source)
        if (loaded == null) {
            isFail = true
            return@LaunchedEffect
        }
        image = loaded
    }
    Box(modifier.clipToBounds()) {
        val current = image
        when {
            current != null -> Image(current, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = contentScale)
            fallback != null -> DfsImage(fallback, Modifier.fillMaxSize(), placeholder = placeholder, contentScale = contentScale)
            placeholder != null -> ImagePlaceholder(placeholder, Modifier.fillMaxSize())
            isFail -> Icon(Icons.Default.Image, contentDescription = null, tint = Color.secondaryText, modifier = Modifier.fillMaxSize().padding(8.dp))
        }
    }
}

/**
 * 图片的加载和内存缓存
 */
object DfsImageLoader {

    /** 解码之后图片最长边的最大像素数,避免大图占用太多内存 */
    private const val MAX_SIZE = 2048

    /** 已解码图片的内存缓存,避免列表滑动时重新解码 */
    private val memoryCache = object : LruCache<String, ImageBitmap>((Runtime.getRuntime().maxMemory() / 6).toInt()) {
        override fun sizeOf(key: String, value: ImageBitmap): Int = value.width * value.height * 4
    }

    /** 请求图片用的线程,请求的都是SDK的本机服务,真正的下载并发由SDK控制 */
    private val executor = Executors.newCachedThreadPool()

    /** 重试的版本号,改变时加载失败的图片会重新加载 */
    var retryVersion by mutableIntStateOf(0)
        private set

    /** 重新加载所有加载失败的图片,比如服务器刚上传完文件还在生成缩略图时,超时失败的图片 */
    fun retryFailed() {
        this.retryVersion += 1
    }

    /** 读取内存里的图片 */
    fun get(key: String): ImageBitmap? = this.memoryCache.get(key)

    /** 清空内存缓存,App进入后台或内存紧张时调用,之后再显示时从磁盘缓存读取 */
    fun clearMemory() {
        this.memoryCache.evictAll()
    }

    /**
     * 加载图片,已经下载完成的图片直接读文件,否则通过SDK的本机服务加载
     */
    suspend fun load(source: FileSource): ImageBitmap? {
        this.memoryCache.get(source.cacheKey)?.let { return it }
        val data = this.loadData(source) ?: return null
        val image = withContext(Dispatchers.Default) {
            decodeBitmap(data, MAX_SIZE)?.also { it.prepareToDraw() }?.asImageBitmap()
        } ?: return null
        this.memoryCache.put(source.cacheKey, image)
        return image
    }

    /** 读取图片数据 */
    private suspend fun loadData(source: FileSource): ByteArray? {
        var local = withContext(Dispatchers.IO) { FileSdkUtil.localSource(source) }
        if (local is LocalSource.File) {
            val path = local.path
            withContext(Dispatchers.IO) { runCatching { File(path).readBytes() }.getOrNull() }?.let { return it }

            //拿到路径之后文件正好被清理掉了,再获取一次,这时会返回本机地址
            local = withContext(Dispatchers.IO) { FileSdkUtil.localSource(source) }
        }
        if (local !is LocalSource.Http) {
            return null
        }
        return this.httpData(local.url)
    }

    /**
     * 通过本机HTTP服务下载,状态码不是200时返回null;页面关闭时断开连接,SDK会自动取消下载
     */
    private suspend fun httpData(url: String): ByteArray? = suspendCancellableCoroutine { cont ->
        val connection = runCatching { URL(url).openConnection() as HttpURLConnection }.getOrNull()
        if (connection == null) {
            cont.resume(null)
            return@suspendCancellableCoroutine
        }
        cont.invokeOnCancellation {
            this.executor.execute { connection.disconnect() }
        }
        this.executor.execute {
            val data = runCatching {
                connection.connectTimeout = 15_000
                connection.readTimeout = 120_000
                if (connection.responseCode != 200) {
                    null
                } else {
                    connection.inputStream.use { input ->
                        val output = ByteArrayOutputStream()
                        input.copyTo(output)
                        output.toByteArray()
                    }
                }
            }.getOrNull()
            connection.disconnect()
            if (cont.isActive) {
                cont.resume(data)
            }
        }
    }

    /**
     * 解码图片,超过最大尺寸时缩小,手机拍的照片按EXIF里的方向旋转
     * Android 9开始用ImageDecoder,支持HEIF并且自动处理方向
     * @param maxSize 最长边的最大像素数
     */
    fun decodeBitmap(data: ByteArray, maxSize: Int): Bitmap? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return runCatching {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(ByteBuffer.wrap(data))) { decoder, info, _ ->
                    val size = info.size
                    val scale = min(1f, maxSize.toFloat() / max(size.width, size.height))
                    if (scale < 1f) {
                        decoder.setTargetSize((size.width * scale).roundToInt(), (size.height * scale).roundToInt())
                    }

                    //软件解码的图片才能在后台线程准备和缩放
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                }
            }.getOrNull()
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(data, 0, data.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            return null
        }
        var sampleSize = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sampleSize * 2) >= maxSize) {
            sampleSize *= 2
        }
        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size, options) ?: return null
        val orientation = runCatching {
            ExifInterface(ByteArrayInputStream(data)).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
        val degrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
        if (degrees == 0) {
            return bitmap
        }
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
