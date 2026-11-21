package cn.dairo.dfs.api.model

import kotlinx.serialization.Serializable

@Serializable
data class FilePropertyHistoryModel(

    /** 文件ID */
    val id: Long,

    /** 大小 */
    val size: String,

    /** 创建日期 */
    val date: String,
)
