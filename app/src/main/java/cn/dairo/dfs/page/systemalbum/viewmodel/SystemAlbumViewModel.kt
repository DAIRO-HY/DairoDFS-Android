package cn.dairo.dfs.page.systemalbum.viewmodel

import android.Manifest
import android.content.ContentUris
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.runtime.getValue
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cn.dairo.dfs.api.FileUploadApi
import cn.dairo.dfs.page.album.AlbumPage
import cn.dairo.dfs.page.systemalbum.model.SystemAlbumItem
import cn.dairo.dfs.store.SettingStore
import cn.dairo.dfs.util.AppActivity
import cn.dairo.dfs.util.FileSdkUtil
import cn.dairo.dfs.util.FileUploadSource
import cn.dairo.dfs.util.NotificationCenter
import cn.dairo.dfs.util.ObservableObject
import cn.dairo.dfs.util.fileSdkMessage
import cn.dairo.file.sdk.Event
import cn.dairo.file.sdk.UploadItem
import cn.dairo.file.sdk.UploadQuery
import cn.dairo.file.sdk.UploadSource
import cn.dairo.file.sdk.UploadState
import cn.dairo.ui.DairoUI
import cn.dairo.ui.root.Toast
import cn.dairo.ui.util.LocalObjectUtil
import cn.dairo.ui.util.fileSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * 上传模式
 */
enum class UploadMode {

    /** 文件的方式上传 */
    FILE,

    /** 相册的方式上传 */
    ALBUM,
}

class SystemAlbumViewModel(

    /** 上传模式 */
    val uploadMode: UploadMode,
) : ObservableObject() {

    companion object {

        /** 相册文件MD5的本地缓存key */
        private const val KEY_MD5 = "album_md5"

        /** 已上传的提示文字,"删除已上传"只会删除这个状态的文件 */
        private const val MSG_UPLOADED = "上传完成"
    }

    /** 当前相册信息列表 */
    var albumList by mutableStateOf(emptyList<SystemAlbumItem>())
        private set

    /** 文件唯一ID对应所在序号 */
    private var identifier2index = HashMap<String, Int>()

    /** 记录当前选中的文件数 */
    var checkedCount by mutableIntStateOf(0)

    /** 标记是否正在上传中 */
    var isUploading by mutableStateOf(false)

    /** 上传数量通知消息 */
    var uploadCountMsg by mutableStateOf<String?>(null)

    /** 是否没有相册权限 */
    var isDenied by mutableStateOf(false)

    /** 上传ID => 相册文件唯一ID */
    private val uploadId2identifier = HashMap<Long, String>()

    /** 还没有结束的上传ID */
    private val uploadingIds = HashSet<Long>()

    /** 正在进行的同步检查 */
    private var checkJob: Job? = null

    /** 相册文件唯一ID对应的MD5 */
    private val md5Map = HashMap<String, String>(LocalObjectUtil.read<Map<String, String>>(KEY_MD5) ?: emptyMap())

    /** 读取相册,按拍摄时间排序,最新的在最后 */
    fun fetchPhotos() {
        this.isDenied = false
        this.scope.launch {
            val list = withContext(Dispatchers.IO) { queryAlbum() }
            identifier2index = HashMap(list.associate { it.identifier to it.index })
            albumList = list
            checkedCount = 0

            //重新加载之后,已经结束的上传不再显示
            uploadId2identifier.keys.retainAll(uploadingIds)
            loadUploading()
        }
    }

    /** 查询相册里的照片和视频 */
    private fun queryAlbum(): List<SystemAlbumItem> {
        val collection = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.MEDIA_TYPE,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.DATE_ADDED,
            MediaStore.MediaColumns.DURATION,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.MediaColumns.DATE_TAKEN else MediaStore.Files.FileColumns.DATE_ADDED,
        )
        val selection = "${MediaStore.Files.FileColumns.MEDIA_TYPE} IN (${MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE}, ${MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO})"
        class Row(val id: Long, val isVideo: Boolean, val name: String, val date: Long, val duration: Long)
        val rows = ArrayList<Row>()
        DairoUI.context.contentResolver.query(collection, projection, selection, null, null)?.use { cursor ->
            while (cursor.moveToNext()) {
                val dateAdded = cursor.getLong(3) * 1000
                val dateTaken = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) cursor.getLong(5) else 0
                rows.add(
                    Row(
                        id = cursor.getLong(0),
                        isVideo = cursor.getInt(1) == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO,
                        name = cursor.getString(2) ?: "",
                        date = if (dateTaken > 0) dateTaken else dateAdded,
                        duration = cursor.getLong(4),
                    )
                )
            }
        }
        rows.sortBy { it.date }
        return rows.mapIndexed { index, row ->
            val base = if (row.isVideo) MediaStore.Video.Media.EXTERNAL_CONTENT_URI else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            SystemAlbumItem(
                id = row.id,
                uri = ContentUris.withAppendedId(base, row.id),
                index = index,
                isVideo = row.isVideo,
                ext = row.name.substringAfterLast('.', "").lowercase(),
                duration = if (row.isVideo) row.duration else 0,
            )
        }
    }

    /** 图片点击事件 */
    fun onItemClick(item: SystemAlbumItem) {
        this.checkedCount += if (item.checked) -1 else 1
        item.checked = !item.checked
    }

    /** 恢复页面关闭之前还没有结束的上传,已经结束的不恢复,"删除已上传"只认这次打开页面之后确认过的结果 */
    private fun loadUploading() {
        val account = FileSdkUtil.dfsAccount ?: return
        val sdk = runCatching { FileSdkUtil.get() }.getOrNull() ?: return
        val query = UploadQuery(account = account, state = null, offset = 0UL, limit = Int.MAX_VALUE.toULong())
        for (item in runCatching { sdk.listUploads(query) }.getOrDefault(emptyList())) {
            val source = item.source as? UploadSource.Custom ?: continue
            val identifier = FileUploadSource.uri(source.id)?.toString() ?: continue
            if (item.state != UploadState.WAITING && item.state != UploadState.UPLOADING && item.state != UploadState.PAUSED) {
                continue
            }
            this.uploadId2identifier[item.id] = identifier
            this.uploadingIds.add(item.id)
            this.setUploadMsg(identifier, this.stateMsg(item.state, item.error))
        }
        this.updateUploadCount()
    }

    /** 上传按钮点击事件 */
    fun onUploadClick(isOnlyCheck: Boolean) {
        val uploading = this.uploadingIds.mapNotNull { this.uploadId2identifier[it] }.toSet()
        val list = this.albumList.filter { it.checked && it.identifier !in uploading }
        if (list.isEmpty()) {
            Toast.show("未选择任何对象")
            return
        }
        if (isOnlyCheck) {
            this.check(list)
        } else {
            this.upload(list)
        }

        //通知相册列表页面更新
        NotificationCenter.post(AlbumPage.ALBUM_PAGE_RELOAD_DATA)
    }

    /** 添加上传任务 */
    private fun upload(list: List<SystemAlbumItem>) {
        val isAlbum = this.uploadMode == UploadMode.ALBUM
        val folder = if (isAlbum) "/相册" else SettingStore.lastOpenFolder
        val time = System.currentTimeMillis() * 1000
        val items = list.mapIndexed { index, item ->
            UploadItem(
                source = UploadSource.Custom(FileUploadSource.makeId(item.uri)),
                dfsPath = folder + "/${time + index}." + item.ext,
                md5 = this.md5Map[item.identifier],
                skipIfExists = isAlbum,//相册同步时,已经有相同的文件就不需要再上传
            )
        }
        try {
            val ids = FileSdkUtil.get().addUploads(items)

            //上一次的上传已经全部结束时,重新计算进度,否则上一次的文件也会算在总数里
            if (this.uploadingIds.isEmpty()) {
                this.uploadId2identifier.clear()
            }
            ids.zip(list).forEach { (id, item) ->
                this.uploadId2identifier[id] = item.identifier
                this.uploadingIds.add(id)
                this.setUploadMsg(item.identifier, "排队中")
            }
            this.updateUploadCount()
        } catch (e: Throwable) {
            Toast.show(e.fileSdkMessage)
        }
    }

    /** 检查选中的文件是否已经上传 */
    private fun check(list: List<SystemAlbumItem>) {
        list.forEach { this.setUploadMsg(it.identifier, "排队中") }
        this.isUploading = true
        this.checkJob = this.scope.launch {
            try {
                list.forEachIndexed { index, item ->
                    uploadCountMsg = "进度:${index + 1}/${list.size}"
                    setUploadMsg(item.identifier, checkOne(item))
                }
            } finally {
                list.filter { it.uploadMsg == "排队中" }.forEach { setUploadMsg(it.identifier, "") }
                checkJob = null
                isUploading = uploadingIds.isNotEmpty()
            }
        }
    }

    /** 检查一个文件是否已经上传,返回提示文字 */
    private suspend fun checkOne(item: SystemAlbumItem): String {
        var md5 = this.md5Map[item.identifier]
        if (md5 == null) {
            this.setUploadMsg(item.identifier, "文件校验中")
            md5 = try {
                withContext(Dispatchers.IO) { FileUploadSource.md5(item.uri) }
            } catch (e: Throwable) {
                return "失败:${e.fileSdkMessage}"
            }
            this.md5Map[item.identifier] = md5
        }
        this.setUploadMsg(item.identifier, "校验上传状态")
        return suspendCancellableCoroutine { cont ->
            val request = FileUploadApi.exists(md5).hide().error {
                cont.resume("失败:$it")
            }.fail {
                cont.resume("失败:${it.msg ?: ""}")
            }.send {
                cont.resume(if (it) MSG_UPLOADED else "未上传")
            }
            cont.invokeOnCancellation { request.cancel() }
        }
    }

    /** 取消所有还没有结束的上传和检查,已经上传的部分服务器会保留,下次可以继续 */
    fun onCancelClick() {
        this.checkJob?.cancel()
        val ids = this.uploadingIds.toList()
        if (ids.isEmpty()) {
            return
        }
        this.scope.launch(Dispatchers.IO) {
            try {
                FileSdkUtil.get().deleteUploads(ids)
            } catch (e: Throwable) {
                Toast.show(e.fileSdkMessage)
            }
        }
    }

    /** SDK事件,更新相册文件的上传状态 */
    fun onFileSdkEvent(event: Event) {
        when (event) {
            is Event.UploadProgress -> {
                val identifier = this.uploadId2identifier[event.id] ?: return
                if (event.hashing) {
                    this.setUploadMsg(identifier, "文件校验中")
                } else {
                    this.setUploadMsg(identifier, event.done.toLong().fileSize + "/" + event.size.toLong().fileSize)
                }
            }

            is Event.UploadStateChanged -> {
                val identifier = this.uploadId2identifier[event.id] ?: return
                this.setUploadMsg(identifier, this.stateMsg(event.state, event.error))
                if (event.state == UploadState.FINISH || event.state == UploadState.SKIPPED || event.state == UploadState.ERROR) {
                    this.uploadingIds.remove(event.id)
                    if (event.state != UploadState.ERROR) {
                        runCatching { FileSdkUtil.get().getUpload(event.id)?.md5 }.getOrNull()?.let { this.md5Map[identifier] = it }
                    }
                    if (event.state == UploadState.FINISH) {//通知相册列表页面更新
                        NotificationCenter.post(AlbumPage.ALBUM_PAGE_RELOAD_DATA)
                    }
                    this.updateUploadCount()
                }
            }

            is Event.UploadRemoved -> {
                for (id in event.ids) {
                    val identifier = this.uploadId2identifier.remove(id) ?: continue
                    if (this.uploadingIds.remove(id)) {
                        this.setUploadMsg(identifier, "")
                    }
                }
                this.updateUploadCount()
            }

            else -> {}
        }
    }

    /** 上传状态的提示文字 */
    private fun stateMsg(state: UploadState, error: String?): String {
        return when (state) {
            UploadState.WAITING -> "排队中"
            UploadState.UPLOADING -> "准备上传"
            UploadState.PAUSED -> "已暂停"
            UploadState.ERROR -> "失败:${FileSdkUtil.friendlyError(error ?: "")}"
            UploadState.FINISH, UploadState.SKIPPED -> MSG_UPLOADED
        }
    }

    /** 更新上传数量提示 */
    private fun updateUploadCount() {
        this.isUploading = this.uploadingIds.isNotEmpty() || this.checkJob != null
        if (this.uploadId2identifier.isEmpty()) {
            this.uploadCountMsg = null
            return
        }
        val total = this.uploadId2identifier.size
        this.uploadCountMsg = "进度:${total - this.uploadingIds.size}/$total"
    }

    /** 设置相册文件的提示文字 */
    private fun setUploadMsg(identifier: String, msg: String) {
        val index = this.identifier2index[identifier] ?: return
        this.albumList.getOrNull(index)?.uploadMsg = msg
    }

    /** 保存计算好的相册文件md5,只保留相册里还存在的文件 */
    fun saveMd5() {
        this.checkJob?.cancel()
        val md5Map = this.md5Map.filterKeys { it in this.identifier2index }
        LocalObjectUtil.write(md5Map, KEY_MD5)
    }

    /** 已经上传的相册文件 */
    val uploadedUris: List<Uri>
        get() = this.albumList.filter { it.uploadMsg == MSG_UPLOADED }.map { it.uri }

    /** Android 10及以下直接删除已经上传的相册文件,需要存储权限 */
    fun deleteUploaded(uris: List<Uri>) {
        val context = DairoUI.context
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            AppActivity.current?.let { ActivityCompat.requestPermissions(it, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 0) }
            Toast.show("请允许存储权限后再删除")
            return
        }
        this.scope.launch {
            val error = withContext(Dispatchers.IO) {
                runCatching { uris.forEach { context.contentResolver.delete(it, null, null) } }.exceptionOrNull()
            }
            if (error == null) {
                fetchPhotos()
                Toast.show("照片已删除")
            } else {
                Toast.show("删除失败: ${error.message ?: "未知错误"}")
            }
        }
    }

    /** 全选点击事件 */
    fun onCheckAllClick() {
        this.albumList.forEach { it.checked = true }
        this.checkedCount = this.albumList.size
    }

    override fun onCleared() {
        this.saveMd5()
        super.onCleared()
    }
}
