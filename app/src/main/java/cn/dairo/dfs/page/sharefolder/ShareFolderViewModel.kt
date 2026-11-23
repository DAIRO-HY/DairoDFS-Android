package cn.dairo.dfs.page.sharefolder

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cn.dairo.dfs.api.FilesApi
import cn.dairo.dfs.api.http.ApiHttpBase
import cn.dairo.dfs.store.SettingStore
import cn.dairo.dfs.util.FileSdkUtil
import cn.dairo.dfs.util.ObservableObject
import cn.dairo.dfs.util.ShareUploadReceiver
import cn.dairo.dfs.util.fileSdkMessage
import cn.dairo.ui.root.Loading
import cn.dairo.ui.root.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 选择分享的文件上传到哪个文件夹,对应iOS的ShareFolderViewModel
 * @param uris 其他App分享的文件
 * @param onFinish 关闭分享页面,count是加入上传的文件数,取消时为0
 */
class ShareFolderViewModel(private val uris: List<Uri>, private val onFinish: (count: Int) -> Unit) : ObservableObject() {

    companion object {

        /** 登录失效时的提示 */
        private const val UNAUTHORIZED_MSG = "登录已失效,请打开DairoDFS重新登录"
    }

    /** 打开页面时登录的账号,没有登录时为null;上传时账号变了就不再上传,避免传到其他账号 */
    val account = FileSdkUtil.dfsAccount

    /** 当前文件夹,空字符串表示根目录 */
    var folder by mutableStateOf(SettingStore.shareLastFolder)
        private set

    /** 当前文件夹下的子文件夹名 */
    var folders by mutableStateOf(emptyList<String>())
        private set

    var isLoading by mutableStateOf(false)
        private set

    /** 加载失败的信息 */
    var loadError by mutableStateOf<String?>(null)
        private set

    /** 是否显示新建文件夹的输入框 */
    var isShowCreateFolder by mutableStateOf(false)

    /** 新建文件夹的名字 */
    var newFolderName by mutableStateOf("")

    /** 上次分享时选择的文件夹,打开失败时回到根目录 */
    private var lastFolder = SettingStore.shareLastFolder

    /** 正在加载的请求,切换文件夹时取消 */
    private var loadRequest: ApiHttpBase? = null

    init {
        this.load()
    }

    /** 页面标题 */
    val title: String
        get() = if (this.folder.isEmpty()) "选择上传位置" else this.folder.substringAfterLast('/')

    /** 上一级文件夹 */
    val parentFolder: String
        get() = this.folder.substringBeforeLast('/', "")

    /** 打开文件夹 */
    fun open(folder: String) {
        this.folder = folder
        this.load()
    }

    /** 加载当前文件夹下的子文件夹 */
    fun load() {
        if (this.account == null) {
            return
        }
        val folder = this.folder
        this.loadRequest?.cancel()
        this.isLoading = true
        this.loadError = null
        this.loadRequest = FilesApi.getList(folder).hide().unauthorized {
            this.onLoadError(folder, UNAUTHORIZED_MSG)
        }.error {
            this.onLoadError(folder, it)
        }.fail {
            if (folder.isNotEmpty() && folder == this.lastFolder) {

                //上次选择的文件夹可能已经被删除,服务器返回错误时回到根目录,网络问题时显示错误让用户重试
                this.lastFolder = ""
                SettingStore.shareLastFolder = ""
                this.open("")
                return@fail
            }
            this.onLoadError(folder, it.msg ?: "获取文件夹失败(${it.code})")
        }.send {
            if (folder != this.folder) {
                return@send
            }
            this.folders = it.list.filter { fm -> !fm.fileFlag }.map { fm -> fm.name }
            this.isLoading = false
        }
    }

    private fun onLoadError(folder: String, msg: String) {
        if (folder != this.folder) {
            return
        }
        this.isLoading = false
        this.loadError = msg
    }

    /** 上传到当前文件夹 */
    fun upload() {
        if (this.account == null || this.account != FileSdkUtil.dfsAccount) {
            Toast.show("登录的账号已变化,请重新分享")
            return
        }
        val folder = this.folder
        Loading.show()
        this.scope.launch {
            try {
                val count = withContext(Dispatchers.IO) { ShareUploadReceiver.save(uris, folder) }
                SettingStore.shareLastFolder = folder
                Loading.hide()
                onFinish(count)
            } catch (e: Throwable) {
                Loading.hide()
                Toast.show("上传失败:" + e.fileSdkMessage)
            }
        }
    }

    /** 在当前文件夹下新建文件夹,成功之后进入新文件夹 */
    fun createFolder() {
        val name = this.newFolderName.trim()

        //清空输入值,避免下次被显示
        this.newFolderName = ""
        if (name.isEmpty()) {
            return
        }
        if (name.contains("/")) {
            Toast.show("文件夹名不能包含/")
            return
        }
        val folder = this.folder + "/" + name
        FilesApi.createFolder(folder).unauthorized {
            Toast.show(UNAUTHORIZED_MSG)
        }.fail {
            Toast.show(it.msg ?: "创建文件夹失败")
        }.send {
            this.open(folder)
        }
    }

    override fun onCleared() {
        this.loadRequest?.cancel()
        super.onCleared()
    }
}
