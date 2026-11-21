package cn.dairo.dfs.store

import cn.dairo.dfs.api.http.ApiHttp
import cn.dairo.dfs.api.model.FileModel
import cn.dairo.ui.root.Toast
import cn.dairo.ui.util.LocalObjectUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

/**
 * 加载文件列表,先显示本地缓存,再从服务器更新
 * 数据量可能很大,缓存读写都不在主线程。所有回调都在主线程
 */
class CachedListLoader {

    companion object {

        /** 读写缓存的线程,串行执行可以保证先读完缓存,再写入新数据 */
        private val cacheDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

        private val scope: CoroutineScope = MainScope()

        /** 删除缓存,在这之后发起的加载不会读到这个缓存 */
        fun deleteCache(cacheName: String) {
            scope.launch(cacheDispatcher) {
                LocalObjectUtil.delete(cacheName)
            }
        }
    }

    /** 最后一次请求的序号,发起新请求之后,之前请求的结果不再回调 */
    private var requestId = 0

    /** 取消当前请求 */
    private var cancelRequest: (() -> Unit)? = null

    /**
     * 加载列表
     * @param cacheName 缓存名,为null时不使用缓存
     * @param http 列表请求
     * @param failMsg 服务器没有返回错误信息时的提示
     * @param toList 将服务器返回的数据转换成列表,不在主线程执行
     * @param noCache 本地没有缓存,需要等待服务器返回
     * @param failed 请求服务器失败
     * @param callback 文件列表
     */
    fun <T> load(
        cacheName: String?,
        http: ApiHttp<T>,
        failMsg: String,
        toList: (data: T) -> List<FileModel>,
        noCache: () -> Unit,
        failed: () -> Unit,
        callback: (list: List<FileModel>) -> Unit,
    ) {
        this.requestId += 1
        val requestId = this.requestId

        //是否已经显示了本地缓存的列表
        var isCacheShown = false

        //得到本地缓存的列表
        scope.launch {
            val cacheList = withContext(cacheDispatcher) {
                cacheName?.let { LocalObjectUtil.read<List<FileModel>>(it) }
            }
            if (requestId != this@CachedListLoader.requestId) {
                return@launch
            }
            if (cacheList != null) {
                isCacheShown = true
                callback(cacheList)
            } else {
                noCache()
            }
        }

        //将上一次的请求关闭
        this.cancelRequest?.invoke()

        val onFail = { msg: String ->
            if (requestId == this.requestId) {
                Toast.show(msg)
                failed()
            }
        }

        //Api请求列表
        val request = http.fail {
            onFail(it.msg ?: failMsg)
        }.error(onFail).finish {
            if (requestId == this.requestId) {
                this.cancelRequest = null
            }
        }
        this.cancelRequest = { request.cancel() }
        request.send { data ->
            scope.launch {
                val (list, isChanged) = withContext(cacheDispatcher) {
                    val list = toList(data)
                    list to (cacheName?.let { LocalObjectUtil.write(list, it) } ?: true)
                }
                if (requestId != this@CachedListLoader.requestId) {
                    return@launch
                }

                //列表没有更新时不需要回调
                if (isChanged || !isCacheShown) {
                    callback(list)
                }
            }
        }
    }
}
