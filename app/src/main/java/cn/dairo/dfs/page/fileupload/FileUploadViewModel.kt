package cn.dairo.dfs.page.fileupload

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cn.dairo.dfs.util.FileSdkUtil
import cn.dairo.dfs.util.ObservableObject
import cn.dairo.dfs.util.fileSdkMessage
import cn.dairo.file.sdk.Event
import cn.dairo.file.sdk.UploadInfo
import cn.dairo.file.sdk.UploadQuery
import cn.dairo.file.sdk.UploadState
import cn.dairo.ui.root.Toast
import cn.dairo.ui.util.fileSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FileUploadViewModel : ObservableObject() {

    companion object {

        /** 列表最多显示的数量 */
        private const val MAX_COUNT = 1000UL
    }

    /** 上传列表 */
    var list by mutableStateOf(emptyList<UploadInfo>())

    /** 上传ID => 进度信息 */
    val progressInfo = mutableStateMapOf<Long, String>()

    /** 当前选中的id */
    var checked by mutableStateOf(emptySet<Long>())

    init {
        this.reload()
    }

    /** 重新加载数据 */
    fun reload() {
        val account = FileSdkUtil.dfsAccount
        if (account == null) {
            this.list = emptyList()
            return
        }
        try {
            val query = UploadQuery(account = account, state = null, offset = 0UL, limit = MAX_COUNT)
            this.list = FileSdkUtil.get().listUploads(query)
        } catch (e: Throwable) {
            Toast.show(e.fileSdkMessage)
        }
    }

    /** SDK事件 */
    fun onFileSdkEvent(event: Event) {
        when (event) {
            is Event.UploadProgress -> {
                val index = this.list.indexOfFirst { it.id == event.id }
                if (index < 0) {
                    return
                }
                val item = this.list[index]
                if (event.hashing) {
                    this.update(index, item.copy(size = event.size))
                    this.progressInfo[event.id] = "文件校验中 ${event.done.toLong().fileSize}"
                } else {
                    this.update(index, item.copy(size = event.size, uploaded = event.done))
                    this.progressInfo[event.id] = "${event.done.toLong().fileSize}(${event.speed.toLong().fileSize}/S)"
                }
            }

            is Event.UploadStateChanged -> {
                val index = this.list.indexOfFirst { it.id == event.id }
                if (index < 0) {
                    this.reload()
                    return
                }
                this.update(index, this.list[index].copy(state = event.state, error = event.error))
                if (event.state != UploadState.UPLOADING) {
                    this.progressInfo.remove(event.id)
                }
            }

            is Event.UploadRemoved -> {
                this.list = this.list.filter { it.id !in event.ids }
                this.checked = this.checked - event.ids.toSet()
            }

            else -> {}
        }
    }

    /** 替换列表里的一条数据 */
    private fun update(index: Int, item: UploadInfo) {
        this.list = this.list.toMutableList().also { it[index] = item }
    }

    /** 选中状态点击事件 */
    fun onCheckClick(id: Long) {
        this.checked = if (id in this.checked) this.checked - id else this.checked + id
    }

    /** 选择所有点击事件 */
    fun onCheckAllClick() {
        this.checked = this.checked + this.list.map { it.id }
    }

    /** 暂停/开始点击事件 */
    fun onStateClick(item: UploadInfo) {
        this.run {
            if (item.state == UploadState.WAITING || item.state == UploadState.UPLOADING) {
                FileSdkUtil.get().pauseUploads(listOf(item.id))
            } else {
                FileSdkUtil.get().resumeUploads(listOf(item.id))
            }
        }
    }

    /** 暂停所有点击事件 */
    fun onPauseAllClick() {
        this.run { FileSdkUtil.get().pauseAllUploads() }
    }

    /** 开始所有点击事件 */
    fun onStartAllClick() {
        this.run { FileSdkUtil.get().resumeAllUploads() }
    }

    /** 删除点击事件,删除会等待任务停止,不能在主线程调用 */
    fun onDeleteClick() {
        val ids = this.checked.toList()
        this.checked = emptySet()
        this.scope.launch {
            withContext(Dispatchers.IO) {
                try {
                    FileSdkUtil.get().deleteUploads(ids)
                } catch (e: Throwable) {
                    Toast.show(e.fileSdkMessage)
                }
            }
            reload()
        }
    }

    /** 执行操作之后刷新列表 */
    private fun run(action: () -> Unit) {
        try {
            action()
        } catch (e: Throwable) {
            Toast.show(e.fileSdkMessage)
        }
        this.reload()
    }
}
