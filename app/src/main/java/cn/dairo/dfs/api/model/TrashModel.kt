package cn.dairo.dfs.api.model

import kotlinx.serialization.Serializable

@Serializable
data class TrashModel(

    /** 文件id */
    val id: Long,

    /** 名称 */
    val name: String,

    /** 大小 */
    val size: Long,

    /** 是否文件 */
    val fileFlag: Boolean,

    /** 剩余时间文字,例如:29天后删除 */
    val date: String,

    /** 是否有缩略图 */
    val hasThumb: Boolean,
)
