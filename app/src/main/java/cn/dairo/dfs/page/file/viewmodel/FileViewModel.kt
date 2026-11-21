package cn.dairo.dfs.page.file.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cn.dairo.dfs.api.FilesApi
import cn.dairo.dfs.api.http.ApiHttp
import cn.dairo.dfs.api.http.EmptyModel
import cn.dairo.dfs.api.model.FileModel
import cn.dairo.dfs.extension.downloadSource
import cn.dairo.dfs.extension.isAudio
import cn.dairo.dfs.extension.isFile
import cn.dairo.dfs.extension.isFolder
import cn.dairo.dfs.extension.isImage
import cn.dairo.dfs.extension.isVideo
import cn.dairo.dfs.page.albumviewer.viewmodel.AlbumViewerViewModel
import cn.dairo.dfs.page.file.entity.FileEntity
import cn.dairo.dfs.store.DfsFileStore
import cn.dairo.dfs.store.SettingStore
import cn.dairo.dfs.util.FileSdkUtil
import cn.dairo.dfs.util.ObservableObject
import cn.dairo.dfs.util.fileSdkMessage
import cn.dairo.ui.root.Toast

class FileViewModel : ObservableObject() {

    /** 当前请求的文件夹 */
    var currentFolder by mutableStateOf("")

    /** 记录当前选中的文件数 */
    var selectedCount by mutableIntStateOf(0)

    var entities by mutableStateOf(emptyList<FileEntity>())

    /** 正在等待服务器返回文件列表,本地有缓存时不需要等待 */
    var isLoading by mutableStateOf(false)

    /** 选择模式 */
    var isSelectMode by mutableStateOf(false)

    /** 是否显示添加视图 */
    var isShowAddView by mutableStateOf(false)

    /** 显示查看相册页面 */
    var showViewerPage by mutableStateOf(false)

    /** 正在播放的音频文件,不为空时显示音频播放页面 */
    var audioFile by mutableStateOf<FileModel?>(null)

    /** 剪切板中的文件ID */
    private val clipboardSet = HashSet<Long>()

    /** 剪切板类型 1:剪切 2:复制 */
    var clipboardType by mutableIntStateOf(0)

    /** 记录当前点击的文件id */
    private var currentClickFileId = -1L

    init {
        this.reload()
    }

    /** 重新加载 */
    fun reload() {
        this.loadSubFile(SettingStore.lastOpenFolder)
    }

    /** 获取文件列表 */
    fun loadSubFile(path: String) {
        val folderPath = if (path == "/") "" else path

        //记录最后打开的文件夹
        SettingStore.lastOpenFolder = folderPath
        this.currentFolder = folderPath.ifEmpty { "根目录" }
        DfsFileStore.getSubList(folderPath, noCache = {

            //不能继续显示上一个文件夹的内容
            this.entities = emptyList()
            this.clearSelected()
            this.isSelectMode = false
            this.isLoading = true
        }, failed = {
            this.isLoading = false
        }) { list ->
            this.isLoading = false
            this.entities = list.map { FileEntity(it) }
            this.clearSelected()
            this.isSelectMode = false
        }
    }

    /** 文件剪切中,显示成半透明 */
    fun isMoveFlag(id: Long): Boolean {
        return this.clipboardType == 1 && id in this.clipboardSet
    }

    /** 文件点击事件 */
    fun onFileClick(item: FileEntity) {
        if (this.isSelectMode) {
            item.isSelected = !item.isSelected
            this.selectedCount += if (item.isSelected) 1 else -1
            return
        }
        if (item.fm.isFolder) {//如果这是一个文件夹
            this.loadSubFile(SettingStore.lastOpenFolder + "/" + item.fm.name)
            return
        }
        if (item.fm.isImage || item.fm.isVideo) {//这个文件是图片或者视频
            this.currentClickFileId = item.fm.id
            this.showViewerPage = true
            return
        }
        if (item.fm.isAudio) {
            this.audioFile = item.fm
        }
    }

    /** 返回上一级 */
    fun onBackClick() {
        val lastOpenFolder = SettingStore.lastOpenFolder
        if (lastOpenFolder.isEmpty()) {
            return
        }
        val folders = lastOpenFolder.split("/").filter { it.isNotEmpty() }.dropLast(1)
        this.loadSubFile("/" + folders.joinToString("/"))
    }

    /** 清空已经选择的文件 */
    fun clearSelected() {
        this.entities.forEach { it.isSelected = false }
        this.selectedCount = 0
    }

    /** 存放剪切板 */
    fun toClipboard(clipboardType: Int) {
        this.entities.filter { it.isSelected }.forEach { this.clipboardSet.add(it.fm.id) }
        this.clipboardType = clipboardType
        Toast.show("操作成功,请选择文件夹后粘贴")
        this.clearSelected()
        this.isSelectMode = false
    }

    /** 粘贴点击事件 */
    fun onPasteClick() {
        val http: ApiHttp<EmptyModel> = if (this.clipboardType == 1) {
            FilesApi.move(this.clipboardSet.toList(), targetFolder = SettingStore.lastOpenFolder, isOverWrite = false)
        } else {
            FilesApi.copy(this.clipboardSet.toList(), targetFolder = SettingStore.lastOpenFolder, isOverWrite = false)
        }
        http.send {
            this.clipboardType = 0
            this.clipboardSet.clear()
            Toast.show("操作成功")
            this.reload()
        }
    }

    /** 当前选中的第一个文件名,重命名时使用 */
    val selectedName: String
        get() = this.entities.firstOrNull { it.isSelected }?.fm?.name ?: ""

    /** 重命名点击事件 */
    fun onRenameClick(newName: String) {

        //当前选中文件
        val id = this.entities.firstOrNull { it.isSelected }?.fm?.id ?: return
        this.isSelectMode = false
        FilesApi.rename(id, newName).send {
            Toast.show("操作成功")
            this.reload()
        }
    }

    /** 下载按钮点击事件 */
    fun onDownloadClick() {
        val downloadList = this.entities.filter { it.isSelected && it.fm.isFile }
        if (downloadList.isEmpty()) {
            Toast.show("请选择要下载的文件,暂不支持文件夹下载")
            return
        }

        //添加到下载列表
        try {
            downloadList.forEach { FileSdkUtil.download(it.fm.downloadSource) }
        } catch (e: Throwable) {
            Toast.show(e.fileSdkMessage)
            return
        }
        Toast.show("已经添加到下载列表")
        this.isSelectMode = false
        this.clearSelected()
    }

    /** 全选 */
    fun selectAll() {
        this.entities.forEach { it.isSelected = true }
        this.selectedCount = this.entities.size
    }

    /** 删除按钮点击事件 */
    fun onDeleteClick() {
        FilesApi.delete(this.entities.filter { it.isSelected }.map { it.fm.id }).send {
            Toast.show("删除成功")

            //当前选中的数量归零
            this.selectedCount = 0
            this.reload()
        }
    }

    /** 获取相册查看页面的ViewModel */
    fun getAlbumViewerViewModel(): AlbumViewerViewModel {

        //当前点击的文件在所有图片和视频中的序号
        var index = -1
        val fileModels = ArrayList<FileModel>()
        for (it in this.entities) {
            val fm = it.fm
            if (fm.isFolder || (!fm.isVideo && !fm.isImage)) {
                continue
            }
            if (fm.id == this.currentClickFileId) {
                index = fileModels.size
            }
            fileModels.add(fm)
        }
        return AlbumViewerViewModel(fileModels, index)
    }
}
