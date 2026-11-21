package cn.dairo.dfs.api

import cn.dairo.dfs.api.http.ApiHttp
import cn.dairo.dfs.api.http.EmptyModel
import cn.dairo.dfs.api.model.LoginAppOutModel
import cn.dairo.dfs.constants.Const

object LoginApi {

    /** Android客户端标识 */
    private const val CLIENT_FLAG = 1

    /** @param pwd 明文密码 */
    fun doLogin(name: String, pwd: String, deviceId: String): ApiHttp<LoginAppOutModel> {
        return ApiHttp<LoginAppOutModel>(
            ApiConst.LOGIN_DO_LOGIN, method = "POST",
            parameter = mapOf("name" to name, "pwd" to pwd, "deviceId" to deviceId, "clientFlag" to CLIENT_FLAG, "clientVersion" to Const.versionCode),
        ).noAuth()
    }

    fun logout(): ApiHttp<EmptyModel> {
        return ApiHttp<EmptyModel>(ApiConst.LOGIN_LOGOUT, method = "DELETE")
    }
}
