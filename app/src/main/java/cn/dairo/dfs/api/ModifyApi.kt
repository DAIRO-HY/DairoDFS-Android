package cn.dairo.dfs.api

import cn.dairo.dfs.api.http.ApiHttp
import cn.dairo.dfs.api.http.EmptyModel

object ModifyApi {

    /** 修改密码,成功之后该用户所有设备的票据都会失效 */
    fun modify(oldPwd: String, pwd: String): ApiHttp<EmptyModel> {
        return ApiHttp<EmptyModel>(ApiConst.MODIFY_PWD, method = "PUT", parameter = mapOf("oldPwd" to oldPwd, "pwd" to pwd))
    }
}
