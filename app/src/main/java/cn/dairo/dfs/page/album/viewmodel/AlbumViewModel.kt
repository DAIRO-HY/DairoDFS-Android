package cn.dairo.dfs.page.album.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cn.dairo.dfs.api.FilesApi
import cn.dairo.dfs.api.model.FileModel
import cn.dairo.dfs.extension.downloadSource
import cn.dairo.dfs.page.album.entity.AlbumEntity
import cn.dairo.dfs.page.albumgroup.entity.AlbumGroup
import cn.dairo.dfs.page.albumgroup.entity.AlbumGroupBuilder
import cn.dairo.dfs.page.albumgroup.entity.AlbumViewMode
import cn.dairo.dfs.page.albumviewer.viewmodel.AlbumViewerViewModel
import cn.dairo.dfs.store.CachedListLoader
import cn.dairo.dfs.store.SettingStore
import cn.dairo.dfs.util.FileSdkUtil
import cn.dairo.dfs.util.ObservableObject
import cn.dairo.dfs.util.fileSdkMessage
import cn.dairo.ui.root.Toast

class AlbumViewModel : ObservableObject() {

    companion object {

        /** 相册列表加载器 */
        private val loader = CachedListLoader()

        /** 相册列表数据缓存名 */
        private val cacheName: String?
            get() = SettingStore.accountObjectName("albums")

        /** 删除相册列表数据缓存 */
        fun deleteCache() {
            cacheName?.let { CachedListLoader.deleteCache(it) }
        }
    }

    /** 显示查看相册页面 */
    var showViewerPage by mutableStateOf(false)

    /** 记录当前选中的文件数 */
    var selectedCount by mutableIntStateOf(0)

    var entityList by mutableStateOf(emptyList<AlbumEntity>())

    /** 相册的显示方式 */
    var viewMode by mutableStateOf(AlbumViewMode.ALL)

    /** 按年显示的分组 */
    var yearGroups by mutableStateOf(emptyList<AlbumGroup>())

    /** 按月显示的分组 */
    var monthGroups by mutableStateOf(emptyList<AlbumGroup>())

    /** 显示全部照片时要定位到的文件ID,没有时定位到最底部 */
    var scrollToId: Long? = null

    /** 正在等待服务器返回相册列表,本地有缓存时不需要等待 */
    var isLoading by mutableStateOf(false)

    /** 选择模式 */
    var isSelectMode by mutableStateOf(false)

    /** 记录当前点击的序号 */
    private var currentClickIndex = -1

    /** 是否需要重新加载数据 */
    var isNeedReloadData = false

    init {
        this.loadData()
    }

    /** 获取文件列表 */
    fun loadData() {

        //不同账号的缓存互相隔离
        loader.load(
            cacheName = cacheName,
            http = FilesApi.getAlbumList().hide(),
            failMsg = "加载相册失败",
            toList = { list -> list.map { it.fileModel } },
            noCache = {

                //已经有内容时,继续显示旧内容,等服务器返回后再刷新
                this.isLoading = this.entityList.isEmpty()
            },
            failed = {
                this.isLoading = false
            },
        ) { modelList ->
            this.isLoading = false
            this.toEntity(modelList)
        }
    }

    /** 查看页面或者上传页面关闭之后,如果有数据变化,重新加载 */
    fun reloadIfNeed() {
        if (!this.isNeedReloadData) {
            return
        }
        this.isNeedReloadData = false

        //删除数据缓存
        deleteCache()
        this.loadData()
    }

    /** 整理列表数据 */
    private fun toEntity(modelList: List<FileModel>) {
        this.entityList = modelList.mapIndexed { i, model -> AlbumEntity(i, model) }
        this.yearGroups = AlbumGroupBuilder.buildYearGroups(modelList)
        this.monthGroups = AlbumGroupBuilder.buildMonthGroups(modelList)
    }

    /** 分组里的照片点击事件,回到全部照片并定位到这张照片 */
    fun onGroupPhotoClick(fm: FileModel) {
        this.scrollToId = fm.id
        this.viewMode = AlbumViewMode.ALL
    }

    /** 清空已经选择的文件 */
    fun clearSelected() {
        this.entityList.forEach { it.isSelected = false }
        this.selectedCount = 0
    }

    /** 全选 */
    fun selectAll() {
        this.entityList.forEach { it.isSelected = true }
        this.selectedCount = this.entityList.size
    }

    /** 相册点击事件 */
    fun onItemClick(entity: AlbumEntity) {
        if (this.isSelectMode) {
            entity.isSelected = !entity.isSelected
            this.selectedCount += if (entity.isSelected) 1 else -1
            return
        }
        this.currentClickIndex = entity.index
        this.isNeedReloadData = false
        this.showViewerPage = true
    }

    /** 获取相册查看页面的ViewModel */
    fun getAlbumViewerViewModel(): AlbumViewerViewModel {
        return AlbumViewerViewModel(this.entityList.map { it.fm }, this.currentClickIndex)
    }

    /** 删除按钮点击事件 */
    fun onDeleteClick() {
        val deleteIds = this.entityList.filter { it.isSelected }.map { it.fm.id }
        FilesApi.delete(deleteIds).send {
            Toast.show("删除成功")

            //删除数据缓存
            deleteCache()
            this.loadData()
        }
    }

    /** 下载按钮点击事件 */
    fun onDownloadClick() {

        //将选中的文件添加到下载列表
        try {
            this.entityList.filter { it.isSelected }.forEach { FileSdkUtil.download(it.fm.downloadSource) }
        } catch (e: Throwable) {
            Toast.show(e.fileSdkMessage)
            return
        }
        Toast.show("已添加到下载列表")
    }
}
