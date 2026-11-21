package cn.dairo.dfs.api

import cn.dairo.dfs.api.http.ApiHttp

object FileUploadApi {

    /**
     * 检查当前用户的文件里是否已经有相同的文件
     * @param md5 文件的md5
     */
    fun exists(md5: String): ApiHttp<Boolean> {
        return ApiHttp<Boolean>(ApiConst.FILE_UPLOAD_EXISTS, method = "GET", parameter = mapOf("md5" to md5))
    }
}
