package cn.dairo.dfs.api.model

import kotlinx.serialization.Serializable

@Serializable
data class FileModel(

    /** 文件id */
    val id: Long,

    /** 名称 */
    val name: String,

    /** 大小 */
    val size: Long,

    /** 是否文件 */
    val fileFlag: Boolean,

    /** 创建日期(毫秒) */
    val date: Long,

    /** 是否有缩略图 */
    val hasThumb: Boolean,

    /** 视频总时长(毫秒),只有相册列表有值 */
    val duration: Long? = null,
)
