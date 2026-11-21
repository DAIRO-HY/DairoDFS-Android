package cn.dairo.dfs.api.model

import kotlinx.serialization.Serializable

@Serializable
data class FileListModel(

    /** 当前文件夹ID */
    val folderId: Long,

    /** 文件列表 */
    val list: List<FileModel>,
)
