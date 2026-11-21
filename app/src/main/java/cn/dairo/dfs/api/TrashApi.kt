package cn.dairo.dfs.api

import cn.dairo.dfs.api.http.ApiHttp
import cn.dairo.dfs.api.http.EmptyModel
import cn.dairo.dfs.api.model.TrashModel

object TrashApi {

    /** 获取回收站文件列表 */
    fun getList(): ApiHttp<List<TrashModel>> {
        return ApiHttp<List<TrashModel>>(ApiConst.TRASH_LIST, method = "GET")
    }

    /**
     * 彻底删除文件
     * @param ids 选中的文件ID列表
     */
    fun delete(ids: List<Long>): ApiHttp<EmptyModel> {
        return ApiHttp<EmptyModel>(ApiConst.TRASH_DELETE, method = "DELETE", parameter = mapOf("ids" to ids))
    }

    /**
     * 从垃圾箱还原文件
     * @param ids 选中的文件ID列表
     */
    fun recover(ids: List<Long>): ApiHttp<EmptyModel> {
        return ApiHttp<EmptyModel>(ApiConst.TRASH_RECOVER, method = "PUT", parameter = mapOf("ids" to ids))
    }
}
