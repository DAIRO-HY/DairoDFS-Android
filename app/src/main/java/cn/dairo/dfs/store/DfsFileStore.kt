package cn.dairo.dfs.store

import cn.dairo.dfs.api.FilesApi
import cn.dairo.dfs.api.model.FileModel

/**
 * DFS文件列表缓存
 */
object DfsFileStore {

    /** 文件列表加载器 */
    private val loader = CachedListLoader()

    /**
     * 获取文件列表,所有回调都在主线程
     * @param noCache 本地没有缓存,需要等待服务器返回
     * @param failed 请求服务器失败
     * @param callback 文件列表
     */
    fun getSubList(folder: String, noCache: () -> Unit, failed: () -> Unit, callback: (files: List<FileModel>) -> Unit) {

        //缓存文件名,不同账号的缓存互相隔离
        val cacheName = SettingStore.accountObjectName("file_list_root_" + folder.replace("/", "_"))
        loader.load(
            cacheName = cacheName,
            http = FilesApi.getList(folder).hide(),
            failMsg = "加载文件列表失败",
            toList = { it.list },
            noCache = noCache,
            failed = failed,
            callback = callback,
        )
    }
}
