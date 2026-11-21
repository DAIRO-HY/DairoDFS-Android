package cn.dairo.dfs.util

import java.io.File

/**
 * 实况照片信息
 */
class DliveInfo(

    /** 照片格式 */
    val photoExt: String,

    /** 照片数据 */
    val photoData: ByteArray,

    /** 视频格式 */
    val videoExt: String,

    /** 视频数据 */
    val videoData: ByteArray,
)

/**
 * 实况照片是把照片和视频合并成一个文件
 * 文件头部格式:dlive|图片格式|图片大小|视频格式|视频大小-
 */
object DliveUtil {

    /** 获取实况照片信息,格式不正确时返回null */
    fun getInfo(path: String): DliveInfo? {
        val data = runCatching { File(path).readBytes() }.getOrNull() ?: return null
        return getInfo(data)
    }

    /** 获取实况照片信息,格式不正确时返回null */
    fun getInfo(data: ByteArray): DliveInfo? {
        return runCatching {

            //获取减号所在的位置
            val headEndIndex = data.indexOf('-'.code.toByte())
            val head = String(data, 0, headEndIndex)
            val headArr = head.split("|")
            val photoExt = headArr[1]
            val photoSize = headArr[2].toInt()
            val photoData = data.copyOfRange(headEndIndex + 1, headEndIndex + 1 + photoSize)
            val videoExt = headArr[3]
            val videoData = data.copyOfRange(headEndIndex + 1 + photoSize, data.size)
            DliveInfo(photoExt = photoExt, photoData = photoData, videoExt = videoExt, videoData = videoData)
        }.getOrNull()
    }
}
