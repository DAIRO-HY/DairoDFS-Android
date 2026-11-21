package cn.dairo.dfs.api.model

import kotlinx.serialization.Serializable

@Serializable
data class LoginAppOutModel(

    /** 登录票据 */
    val token: String,

    /** 用户名 */
    val name: String,

    /** 是否管理员 */
    val isAdmin: Boolean,
)
