package cn.dairo.dfs.api.model

import kotlinx.serialization.Serializable

@Serializable
data class FilePropertyModel(

    /** 文件ID */
    val id: Long,

    /** 名称 */
    val name: String,

    /** 路径 */
    val path: String,

    /** 大小 */
    val size: String,

    /** 文件属性 */
    val property: String,

    /** 文件类型(文件专用) */
    val contentType: String,

    /** 创建日期 */
    val date: String,

    /** 上传时间(毫秒),复制得到的文件是复制时的时间 */
    val uploadDate: Long,

    /** 文件状态 */
    val state: Int,

    /** 文件处理结果信息 */
    val stateMsg: String? = null,

    /** 是否文件 */
    val isFile: Boolean,

    /** 文件数(文件夹属性专用) */
    val fileCount: Int? = null,

    /** 文件夹数(文件夹属性专用) */
    val folderCount: Int? = null,

    /** 历史记录(文件属性专用) */
    val historyList: List<FilePropertyHistoryModel>,

    /** 扩展文件列表 */
    val extraList: List<FilePropertyExtraModel>,
)
