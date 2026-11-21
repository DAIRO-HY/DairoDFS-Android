package cn.dairo.dfs.page.filedownload

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cn.dairo.dfs.util.FileSdkUtil
import cn.dairo.dfs.util.ObservableObject
import cn.dairo.dfs.util.fileSdkMessage
import cn.dairo.file.sdk.CacheInfo
import cn.dairo.file.sdk.CacheMode
import cn.dairo.file.sdk.CacheQuery
import cn.dairo.file.sdk.DownloadState
import cn.dairo.file.sdk.Event
import cn.dairo.ui.root.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FileDownloadViewModel : ObservableObject() {

    companion object {

        /** 列表最多显示的数量 */
        private const val MAX_COUNT = 1000UL
    }

    /** 当前显示的模式 下载或缓存 */
    var mode by mutableStateOf(CacheMode.SAVE)

    /** 文件列表 */
    var list by mutableStateOf(emptyList<CacheInfo>())

    /** 文件ID => 下载速度 */
    val speeds = mutableStateMapOf<String, ULong>()

    /** 当前选中的id */
    var checked by mutableStateOf(emptySet<String>())

    init {
        this.reload()
    }

    /** 重新加载数据 */
    fun reload() {
        try {
            val query = CacheQuery(group = null, mode = this.mode, state = null, offset = 0UL, limit = MAX_COUNT)
            this.list = FileSdkUtil.get().list(query)
        } catch (e: Throwable) {
            Toast.show(e.fileSdkMessage)
        }
    }

    /** 切换下载和缓存 */
    fun onModeClick(mode: CacheMode) {
        this.mode = mode
        this.checked = emptySet()
        this.reload()
    }

    /** SDK事件 */
    fun onFileSdkEvent(event: Event) {
        when (event) {
            is Event.Progress -> {
                val index = this.list.indexOfFirst { it.id == event.id }
                if (index < 0) {
                    return
                }
                this.update(index, this.list[index].copy(size = event.size, downloaded = event.downloaded))
                this.speeds[event.id] = event.speed
            }

            is Event.StateChanged -> {
                val index = this.list.indexOfFirst { it.id == event.id }
                if (index < 0) {
                    if (event.mode == this.mode) {//新添加的文件
                        this.reload()
                    }
                    return
                }
                this.update(index, this.list[index].copy(state = event.state, error = event.error))
                if (event.state != DownloadState.DOWNLOADING) {
                    this.speeds.remove(event.id)
                }
            }

            is Event.Removed -> {
                this.list = this.list.filter { it.id !in event.ids }
                this.checked = this.checked - event.ids.toSet()
            }

            else -> {}
        }
    }

    /** 替换列表里的一条数据 */
    private fun update(index: Int, item: CacheInfo) {
        this.list = this.list.toMutableList().also { it[index] = item }
    }

    /** 选中状态点击事件 */
    fun onCheckClick(id: String) {
        this.checked = if (id in this.checked) this.checked - id else this.checked + id
    }

    /** 选择所有点击事件 */
    fun onCheckAllClick() {
        this.checked = this.checked + this.list.map { it.id }
    }

    /** 暂停/开始点击事件 */
    fun onStateClick(item: CacheInfo) {
        this.run {
            if (item.state == DownloadState.WAITING || item.state == DownloadState.DOWNLOADING) {
                FileSdkUtil.get().pause(listOf(item.id))
            } else {
                FileSdkUtil.get().resume(listOf(item.id))
            }
        }
    }

    /** 暂停所有点击事件 */
    fun onPauseAllClick() {
        this.run { FileSdkUtil.get().pauseAll() }
    }

    /** 开始所有点击事件 */
    fun onStartAllClick() {
        this.run { FileSdkUtil.get().resumeAll() }
    }

    /** 删除点击事件,删除会等待任务停止,不能在主线程调用 */
    fun onDeleteClick() {
        val ids = this.checked.toList()
        this.checked = emptySet()
        this.scope.launch {
            withContext(Dispatchers.IO) {
                try {
                    FileSdkUtil.get().delete(ids)
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
