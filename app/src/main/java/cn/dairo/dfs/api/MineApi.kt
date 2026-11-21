package cn.dairo.dfs.api

import cn.dairo.dfs.api.http.ApiHttp
import cn.dairo.dfs.api.http.EmptyModel
import cn.dairo.dfs.api.model.MineModel

object MineApi {

    /** 获取当前用户信息 */
    fun info(): ApiHttp<MineModel> {
        return ApiHttp<MineModel>(ApiConst.MINE_INFO, method = "GET")
    }

    /** 修改昵称,可以为空 */
    fun nickname(nickname: String): ApiHttp<EmptyModel> {
        return ApiHttp<EmptyModel>(ApiConst.MINE_NICKNAME, method = "PUT", parameter = mapOf("nickname" to nickname))
    }

    /** 上传头像,服务器会裁剪成正方形 */
    fun uploadAvatar(jpeg: ByteArray): ApiHttp<EmptyModel> {
        return ApiHttp<EmptyModel>(ApiConst.MINE_AVATAR, method = "POST").file(name = "file", fileName = "avatar.jpg", mimeType = "image/jpeg", data = jpeg)
    }

    /** 删除头像 */
    fun deleteAvatar(): ApiHttp<EmptyModel> {
        return ApiHttp<EmptyModel>(ApiConst.MINE_AVATAR, method = "DELETE")
    }
}
