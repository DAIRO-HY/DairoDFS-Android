package cn.dairo.dfs.api.model

import kotlinx.serialization.Serializable

@Serializable
data class MineModel(

    /** 主键 */
    val id: Long,

    /** 用户名 */
    val name: String,

    /** 用户电子邮箱 */
    val email: String,

    /** 创建日期 */
    val date: String,

    /** 用户文件访问路径前缀 */
    val urlPath: String,

    /** API操作TOKEN */
    val apiToken: String,

    /** 端对端加密密钥 */
    val encryptionKey: String,

    /** 是否管理员 */
    val isAdmin: Boolean,

    /** 昵称 */
    val nickname: String? = null,

    /** 头像的MD5,空表示没有头像 */
    val avatar: String? = null,
) {

    /** 显示的名字,没有昵称时显示用户名 */
    val displayName: String
        get() = if (!this.nickname.isNullOrEmpty()) this.nickname else this.name
}
