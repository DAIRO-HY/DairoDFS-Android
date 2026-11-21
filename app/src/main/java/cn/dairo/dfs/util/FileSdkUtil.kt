package cn.dairo.dfs.util

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import cn.dairo.dfs.api.ApiConst
import cn.dairo.dfs.api.model.MineModel
import cn.dairo.dfs.store.SettingStore
import cn.dairo.file.sdk.CacheMode
import cn.dairo.file.sdk.Event
import cn.dairo.file.sdk.EventListener
import cn.dairo.file.sdk.FetchListener
import cn.dairo.file.sdk.FetchTask
import cn.dairo.file.sdk.FileSdk
import cn.dairo.file.sdk.LocalSource
import cn.dairo.file.sdk.Options
import cn.dairo.file.sdk.SdkConfig
import cn.dairo.file.sdk.SdkException
import cn.dairo.file.sdk.UploadQuery
import cn.dairo.file.sdk.UploadState
import cn.dairo.ui.DairoUI
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import java.io.File

/**
 * 文件下载缓存上传SDK,整个App共用一个实例
 */
object FileSdkUtil {

    private const val TAG = "FileSdkUtil"

    /** SDK数据目录名 */
    private const val ROOT_FOLDER = "dairo-file-sdk"

    /** SDK事件,在主线程发送,对应iOS的FileSdkUtil.EVENT通知 */
    private val mEvents = MutableSharedFlow<Event>(extraBufferCapacity = 1024)

    /** SDK事件 */
    val events: SharedFlow<Event> = mEvents

    /** SDK实例的创建结果,创建失败时,在使用的地方抛出错误 */
    private val result: Result<FileSdk> by lazy { runCatching { create() } }

    /** 事件监听,SDK不会持有它之外的引用 */
    private val eventListener = FileSdkEventListener()

    /**
     * 获取SDK实例
     */
    fun get(): FileSdk {
        return result.getOrThrow()
    }

    /**
     * App回到前台时调用,本机HTTP服务失效时会重启,之前拿到的本机地址依然有效
     */
    fun checkServer() {
        try {
            get().checkServer()
        } catch (e: Throwable) {
            Log.e(TAG, "检查SDK本机HTTP服务失败", e)
        }
    }

    /**
     * App进入后台时调用
     */
    fun flush() {
        try {
            get().flush()
        } catch (e: Throwable) {
            Log.e(TAG, "SDK保存数据失败", e)
        }
    }

    /**
     * 当前上传账号,上传任务按账号区分
     */
    val dfsAccount: String?
        get() {
            if (!SettingStore.isLogin) {
                return null
            }
            val user = SettingStore.user ?: return null
            return "${SettingStore.clusterId}:${user.id}"
        }

    /**
     * 登录状态变化之后调用,只有当前账号的任务会上传
     */
    fun syncDfsAccount() {
        val sdk = runCatching { get() }.getOrNull() ?: return
        applyDfsAccount(sdk)
    }

    /**
     * 重新登录之后继续上传失败的任务,这些任务可能是因为票据失效而失败的
     */
    fun resumeFailedUploads() {
        val account = this.dfsAccount ?: return
        val sdk = runCatching { get() }.getOrNull() ?: return
        val query = UploadQuery(account = account, state = UploadState.ERROR, offset = 0UL, limit = Int.MAX_VALUE.toULong())
        runCatching {
            sdk.resumeUploads(sdk.listUploads(query).map { it.id })
        }
    }

    private fun applyDfsAccount(sdk: FileSdk) {
        val account = this.dfsAccount
        if (account != null) {
            sdk.setDfsAccount(account, SettingStore.domainNotNull, SettingStore.token)
        } else {
            sdk.clearDfsAccount()
        }
    }

    /**
     * 获取已经缓存的文件路径,没有缓存时返回null
     */
    fun cachedPath(source: FileSource): String? {
        return runCatching { get().cachedPath(source.url, source.options()) }.getOrNull()
    }

    /**
     * 获取缓存ID,用来和事件、列表里的记录对应
     */
    fun cacheId(source: FileSource): String? {
        return runCatching { get().cacheId(source.url, source.options()) }.getOrNull()
    }

    /**
     * 获取图片的访问方式,适合列表里大量的图片
     * 已经下载完成的返回文件路径,否则返回无状态的本机地址,连接断开时SDK会自动取消下载
     */
    fun localSource(source: FileSource): LocalSource? {
        return runCatching { get().localSource(source.url, source.options()) }.getOrNull()
    }

    /**
     * 获取给播放器使用的本机地址,支持边下边播
     */
    fun open(source: FileSource): String? {
        return runCatching { get().open(source.url, source.options()) }.getOrNull()
    }

    /**
     * 添加永久下载,进度通过事件通知,App重启后自动继续
     */
    fun download(source: FileSource) {
        get().download(source.url, source.options(CacheMode.SAVE))
    }

    /**
     * 把数据存入缓存
     */
    fun putData(source: FileSource, data: ByteArray): String? {
        return runCatching { get().putData(source.url, source.options(), data) }.getOrNull()
    }

    /**
     * 下载并缓存文件,回调都在主线程,取消之后不会再回调
     * @param progress 下载进度(0到100)
     * @param finish 下载结束,成功时返回文件路径,失败时返回错误
     */
    fun fetch(source: FileSource, progress: (percent: Int) -> Unit, finish: (path: String?, error: Throwable?) -> Unit): FetchTask? {
        return try {
            get().fetch(source.url, source.options(), FileSdkFetchListener(progress, finish))
        } catch (e: Throwable) {
            finish(null, e)
            null
        }
    }

    /**
     * 将SDK的错误信息转换成给用户看的提示,没有权限时SDK返回的是地址过期或文件不存在
     */
    fun friendlyError(msg: String): String {
        if (msg.startsWith("地址已过期或者没有权限")) {
            return "没有访问权限或登录已失效"
        }
        if (msg == "文件不存在") {
            return "文件不存在或没有访问权限"
        }
        return msg
    }

    /** 发送事件 */
    internal fun post(event: Event) {
        mEvents.tryEmit(event)
    }

    /**
     * 创建SDK实例,数据目录用noBackupFilesDir,不会被系统清理,也不会备份
     */
    private fun create(): FileSdk {
        val root = File(DairoUI.context.noBackupFilesDir, ROOT_FOLDER)
        root.mkdirs()
        val sdk = FileSdk(SdkConfig(rootDir = root.absolutePath))
        sdk.setListener(this.eventListener)

        //SDK不负责保存设置,启动时再设置一次
        if (SettingStore.fileSdkMaxSavingCount > 0) {
            sdk.setMaxSavingCount(SettingStore.fileSdkMaxSavingCount.toUInt())
        }
        if (SettingStore.fileSdkMaxCachingCount > 0) {
            sdk.setMaxCachingCount(SettingStore.fileSdkMaxCachingCount.toUInt())
        }
        SettingStore.fileSdkCacheExpireDays?.let {
            sdk.setCacheExpireDays(it.toUInt())
        }
        if (SettingStore.fileSdkMaxUploadingCount > 0) {
            sdk.setMaxUploadingCount(SettingStore.fileSdkMaxUploadingCount.toUInt())
        }

        //设置上传账号之后,未完成的上传任务会继续,所以要先准备好上传数据
        sdk.setUploadSourceProvider(FileUploadSource())
        applyDfsAccount(sdk)
        return sdk
    }
}

/**
 * 页面上监听SDK事件,对应iOS的onReceive(FileSdkUtil.EVENT),页面关闭时停止监听
 */
@Composable
fun OnFileSdkEvent(block: (event: Event) -> Unit) {
    val currentBlock = rememberUpdatedState(block)
    LaunchedEffect(Unit) {
        FileSdkUtil.events.collect { currentBlock.value(it) }
    }
}

/**
 * 要通过SDK访问的DFS文件
 */
data class FileSource(

    /** 下载地址 */
    val url: String,

    /** 缓存key,不包含文件名和域名,改名和切换服务器地址之后缓存依然有效 */
    val cacheKey: String,

    /** 缓存分组,用于统计和清理 */
    val group: String,

    /** 文件名,用来决定缓存文件的扩展名和下载列表显示的名字 */
    val name: String,

    /** 下载优先级,数值越大越先下载 */
    val priority: Int = 0,
) {

    /** SDK访问参数 */
    fun options(cache: CacheMode = CacheMode.CACHE): Options {
        return Options(
            cache = cache,
            group = this.group,
            namespace = SettingStore.clusterId,
            cacheKey = this.cacheKey,
            headers = this.headers,
            priority = this.priority,
            name = this.name,
        )
    }

    /** 访问服务器时的请求头,播放器直接播放服务器上的文件时也使用 */
    val headers: Map<String, String>
        get() = mapOf("Authorization" to "Bearer " + SettingStore.token)

    companion object {

        /** 缩略图 */
        fun thumb(id: Long): FileSource {
            return FileSource(
                url = SettingStore.domainNotNull + "/api/files/thumb/$id",
                cacheKey = "/api/files/extra/$id/thumb",
                group = "thumb",
                name = "$id-thumb.jpg",

                //SDK同时下载的文件数有限,缩略图很小,不能排在在线生成的图片后面,否则在线生成很慢时,列表里一张图片也看不到
                priority = 1,
            )
        }

        /** 用户头像,没有头像时返回null,头像的MD5放在缓存key里,换头像之后会重新下载 */
        fun avatar(user: MineModel): FileSource? {
            val avatar = user.avatar
            if (avatar.isNullOrEmpty()) {
                return null
            }
            return FileSource(
                url = SettingStore.domainNotNull + ApiConst.AVATAR + "/${user.id}?v=$avatar",
                cacheKey = ApiConst.AVATAR + "/${user.id}/$avatar",
                group = "avatar",
                name = "${user.id}-avatar.jpg",
            )
        }
    }
}

/**
 * SDK事件监听,转发到主线程
 */
private class FileSdkEventListener : EventListener {

    private val handler = Handler(Looper.getMainLooper())

    override fun onEvent(event: Event) {
        this.handler.post { FileSdkUtil.post(event) }
    }
}

/**
 * 下载回调,转发到主线程
 */
private class FileSdkFetchListener(
    private val progress: (percent: Int) -> Unit,
    private val finish: (path: String?, error: Throwable?) -> Unit,
) : FetchListener {

    private val handler = Handler(Looper.getMainLooper())

    override fun onProgress(size: ULong?, downloaded: ULong) {
        if (size == null || size == 0UL) {
            return
        }
        val percent = (downloaded.toDouble() / size.toDouble() * 100).toInt()
        this.handler.post { this.progress(percent) }
    }

    override fun onSuccess(path: String) {
        this.handler.post { this.finish(path, null) }
    }

    override fun onError(error: SdkException) {
        if (error is SdkException.Cancelled) {
            return
        }
        this.handler.post { this.finish(null, error) }
    }
}

/**
 * SDK的错误信息
 */
val Throwable.fileSdkMessage: String
    get() {
        val msg = when (this) {
            is SdkException.UrlExpired -> this.msg
            is SdkException.Http -> this.msg
            is SdkException.NotFound -> this.msg
            is SdkException.Server -> this.msg
            is SdkException.Source -> this.msg
            is SdkException.Network -> this.msg
            is SdkException.Io -> this.msg
            is SdkException.Md5Mismatch -> this.msg
            is SdkException.InvalidUrl -> this.msg
            is SdkException.Db -> this.msg
            is SdkException.NoAccount -> this.msg
            is SdkException.Cancelled -> this.msg
            else -> this.message ?: this.toString()
        }
        return FileSdkUtil.friendlyError(msg)
    }
