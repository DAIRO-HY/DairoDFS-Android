package cn.dairo.dfs.api

import cn.dairo.dfs.api.http.ApiHttp
import cn.dairo.dfs.api.model.GlobalModel

object CommonApi {

    /** 获取服务器公共信息,不需要登录 */
    fun global(): ApiHttp<GlobalModel> {
        return ApiHttp<GlobalModel>(ApiConst.COMMON_GLOBAL, method = "GET").noAuth()
    }
}
