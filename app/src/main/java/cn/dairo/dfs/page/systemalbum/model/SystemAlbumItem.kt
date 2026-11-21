package cn.dairo.dfs.page.systemalbum.model

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * 系统相册里的一个照片或视频
 */
class SystemAlbumItem(

    /** 相册里的ID */
    val id: Long,

    /** 文件地址,作为相册文件的唯一标识 */
    val uri: Uri,

    /** 当前对象所在索引 */
    val index: Int,

    /** 是否视频 */
    val isVideo: Boolean,

    /** 文件后缀 */
    val ext: String,

    /** 视频时长(毫秒),图片为0 */
    val duration: Long,
) {

    /** 相册文件的唯一标识 */
    val identifier: String
        get() = this.uri.toString()

    /** 上传消息 */
    var uploadMsg by mutableStateOf("")

    /** 是否选中 */
    var checked by mutableStateOf(false)
}
