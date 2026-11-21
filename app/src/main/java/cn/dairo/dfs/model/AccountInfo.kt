package cn.dairo.dfs.model

import kotlinx.serialization.Serializable

/**
 * 登录信息
 */
@Serializable
data class AccountInfo(

    /** 服务器名 */
    val domain: String,

    /** 用户名 */
    val name: String,

    /** 登录票据,退出登录之后为空 */
    val token: String,

    /** 集群ID */
    val clusterId: String,

    /** 是否当前登录的账号 */
    val isCurrent: Boolean = false,
)
