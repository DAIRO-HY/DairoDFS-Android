package cn.dairo.dfs.api

import cn.dairo.dfs.api.http.ApiHttp
import cn.dairo.dfs.api.http.EmptyModel
import cn.dairo.dfs.api.model.AlbumItemModel
import cn.dairo.dfs.api.model.FileListModel
import cn.dairo.dfs.api.model.FilePropertyModel

object FilesApi {

    /**
     * 获取文件列表
     * @param folder 文件夹路径,空字符串表示根目录
     */
    fun getList(folder: String): ApiHttp<FileListModel> {
        return ApiHttp<FileListModel>(ApiConst.FILES_LIST, method = "GET", parameter = mapOf("folder" to folder))
    }

    /** 获取相册列表 */
    fun getAlbumList(): ApiHttp<List<AlbumItemModel>> {
        return ApiHttp<List<AlbumItemModel>>(ApiConst.FILES_ALBUM, method = "GET")
    }

    /** 创建文件夹 */
    fun createFolder(folder: String): ApiHttp<EmptyModel> {
        return ApiHttp<EmptyModel>(ApiConst.FILES_CREATE_FOLDER, method = "POST", parameter = mapOf("folder" to folder))
    }

    /** 删除文件 */
    fun delete(ids: List<Long>): ApiHttp<EmptyModel> {
        return ApiHttp<EmptyModel>(ApiConst.FILES_DELETE, method = "DELETE", parameter = mapOf("ids" to ids))
    }

    /**
     * 重命名
     * @param id 文件ID
     * @param name 新名称
     */
    fun rename(id: Long, name: String): ApiHttp<EmptyModel> {
        return ApiHttp<EmptyModel>(ApiConst.FILES_RENAME, method = "PUT", parameter = mapOf("id" to id, "name" to name))
    }

    /**
     * 修改拍摄时间
     * @param id 文件ID
     * @param date 拍摄时间(毫秒),0表示恢复成照片本身的拍摄时间
     */
    fun updateCaptureTime(id: Long, date: Long): ApiHttp<FilePropertyModel> {
        return ApiHttp<FilePropertyModel>(ApiConst.FILES_CAPTURE_TIME, method = "PUT", parameter = mapOf("id" to id, "date" to date))
    }

    /**
     * 文件复制
     * @param ids 文件ID列表
     * @param targetFolder 目标文件夹
     * @param isOverWrite 是否覆盖目标文件
     */
    fun copy(ids: List<Long>, targetFolder: String, isOverWrite: Boolean): ApiHttp<EmptyModel> {
        return ApiHttp<EmptyModel>(ApiConst.FILES_COPY, method = "POST", parameter = mapOf("ids" to ids, "targetFolder" to targetFolder, "isOverWrite" to isOverWrite))
    }

    /**
     * 文件移动
     * @param ids 文件ID列表
     * @param targetFolder 目标文件夹
     * @param isOverWrite 是否覆盖目标文件
     */
    fun move(ids: List<Long>, targetFolder: String, isOverWrite: Boolean): ApiHttp<EmptyModel> {
        return ApiHttp<EmptyModel>(ApiConst.FILES_MOVE, method = "POST", parameter = mapOf("ids" to ids, "targetFolder" to targetFolder, "isOverWrite" to isOverWrite))
    }

    /**
     * 文件或文件夹属性
     * @param ids 选择的文件ID列表
     */
    fun getProperty(ids: List<Long>): ApiHttp<FilePropertyModel> {
        return ApiHttp<FilePropertyModel>(ApiConst.FILES_PROPERTY, method = "GET", parameter = mapOf("ids" to ids))
    }
}
