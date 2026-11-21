package cn.dairo.dfs.extension

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.ui.graphics.vector.ImageVector
import cn.dairo.dfs.api.ApiConst
import cn.dairo.dfs.api.model.FileModel
import cn.dairo.dfs.store.SettingStore
import cn.dairo.dfs.util.FileSource
import cn.dairo.ui.DairoUI
import java.net.URLEncoder
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * 附属文件
 * @param extra 附属文件名,例如:thumb、preview、preview-dlive
 * @param ext 附属文件的扩展名
 */
private fun FileModel.extraSource(extra: String, ext: String): FileSource {
    return FileSource(
        url = SettingStore.domainNotNull + "/api/files/extra/${this.id}/$extra",
        cacheKey = "/api/files/extra/${this.id}/$extra",
        group = extra,
        name = "${this.id}-$extra.$ext",
    )
}

/** 原文件 */
val FileModel.downloadSource: FileSource
    get() {
        val name = URLEncoder.encode(this.name, "UTF-8").replace("+", "%20")
        return FileSource(
            url = SettingStore.domainNotNull + "/api/files/download/${this.id}/$name",
            cacheKey = "/api/files/download/${this.id}",
            group = "file",
            name = this.name,
        )
    }

/** 缩略图 */
val FileModel.thumbSource: FileSource
    get() = FileSource.thumb(this.id)

/**
 * 在线生成的图片,从中间裁剪成指定的大小
 * @param width 宽度,单位是像素
 * @param height 高度,单位是像素
 */
fun FileModel.thumbOnlineSource(width: Float, height: Float): FileSource {

    //布局计算出来的大小有浮点误差,四舍五入才能保证每次都一样,否则缓存对不上会重新加载
    val w = width.roundToInt()
    val h = height.roundToInt()
    return this.thumbOnlineSource("width=$w&height=$h", "${w}x$h")
}

/**
 * 在线生成的图片,按比例缩放
 * @param maxSize 最大边,单位是像素
 */
fun FileModel.thumbOnlineSource(maxSize: Int): FileSource {
    return this.thumbOnlineSource("max_size=$maxSize", "max$maxSize")
}

/** 在线生成的图片,不同大小要分开缓存 */
private fun FileModel.thumbOnlineSource(query: String, name: String): FileSource {
    val path = "/api/files/thumb_online/${this.id}?$query"
    return FileSource(
        url = SettingStore.domainNotNull + path,
        cacheKey = path,
        group = "thumb-online",
        name = "${this.id}-$name.jpg",
    )
}

/** 文件属性 */
val FileModel.propertySource: FileSource
    get() = FileSource(
        url = SettingStore.domainNotNull + ApiConst.FILES_PROPERTY + "?ids=${this.id}",
        cacheKey = ApiConst.FILES_PROPERTY + "/${this.id}",
        group = "property",
        name = "${this.id}-property.json",
    )

/** 图片预览 */
val FileModel.previewImageSource: FileSource
    get() {
        val lowerName = this.name.lowercase()
        val isNeedPreview = listOf(".psd", ".psb", ".ai", ".cr3", ".cr2", ".dlive").any { lowerName.endsWith(it) }
        return if (isNeedPreview) {//这些格式不能直接显示,要使用服务器生成的预览图
            this.extraSource("preview", "jpg")
        } else {
            this.downloadSource
        }
    }

/** 适合当前屏幕的图片,用于快速浏览 */
val FileModel.screenImageSource: FileSource
    get() {
        val metrics = DairoUI.context.resources.displayMetrics
        return this.thumbOnlineSource(max(metrics.widthPixels, metrics.heightPixels))
    }

/** 流畅画质的预览视频 */
val FileModel.previewVideoSource: FileSource
    get() = this.extraSource("preview", "mp4")

/** 实况照片预览视频 */
val FileModel.dliveVideoPreviewSource: FileSource
    get() = this.extraSource("preview-dlive", "mp4")

/**
 * 从已下载的实况照片里拆出来的视频,只存在于本地缓存
 * @param ext 视频扩展名,只在存入缓存时起作用
 */
fun FileModel.dliveVideoSource(ext: String = "mov"): FileSource {
    return FileSource(
        url = SettingStore.domainNotNull + "/api/files/download/${this.id}/dlive-video.$ext",
        cacheKey = "/dlive-video/${this.id}",
        group = "dlive",
        name = "${this.id}-dlive.$ext",
    )
}

/** 图片显示出来之前的占位图标 */
val FileModel.placeholderIcon: ImageVector
    get() = if (this.isVideo) Icons.Default.Videocam else Icons.Default.Image

/** 这是否是一个视频文件 */
val FileModel.isVideo: Boolean
    get() {
        val lowerName = this.name.lowercase()
        return lowerName.endsWith(".mov") || lowerName.endsWith(".mp4")
    }

/** 这是否是一个图片文件 */
val FileModel.isImage: Boolean
    get() {
        val lowerName = this.name.lowercase()
        return listOf(".jpg", ".jpeg", ".png", ".heic", ".hif", ".gif", ".psd", ".cr3", ".tiff", ".dlive").any { lowerName.endsWith(it) }
    }

/** 这是否是一个音频文件 */
val FileModel.isAudio: Boolean
    get() {
        val lowerName = this.name.lowercase()
        return listOf(".mp3", ".m4a", ".aac", ".wav", ".flac").any { lowerName.endsWith(it) }
    }

/** 这是否是实况照片 */
val FileModel.isDlive: Boolean
    get() = this.name.lowercase().endsWith(".dlive")

/** 是否文件夹 */
val FileModel.isFolder: Boolean
    get() = !this.fileFlag

/** 是否文件 */
val FileModel.isFile: Boolean
    get() = this.fileFlag
