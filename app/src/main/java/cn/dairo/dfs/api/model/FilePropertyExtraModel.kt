package cn.dairo.dfs.api.model

import kotlinx.serialization.Serializable

@Serializable
data class FilePropertyExtraModel(

    /** 文件ID */
    val id: Long,

    /** 扩展文件名 */
    val name: String,

    /** 大小 */
    val size: String,

    /** 文件类型 */
    val contentType: String,
)
