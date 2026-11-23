package cn.dairo.dfs.util

import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import cn.dairo.file.sdk.Event
import cn.dairo.file.sdk.UploadItem
import cn.dairo.file.sdk.UploadQuery
import cn.dairo.file.sdk.UploadSource
import cn.dairo.file.sdk.UploadState
import cn.dairo.ui.DairoUI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * 接收其他App分享的文件,对应iOS的ShareUploadReceiver和ShareFileSaver
 * 其他App给的content://地址只在分享页面打开期间可以读取,所以先复制到App自己的目录,再交给SDK上传
 * 上传完成或者跳过之后删除复制的文件,上传任务被删除的文件在App启动和任务删除时清理
 */
object ShareUploadReceiver {

    private const val TAG = "ShareUploadReceiver"

    /** 复制文件的目录名,每次分享一个子目录 */
    private const val FOLDER = "share-upload"

    private val scope = MainScope()

    /** 复制文件和清理文件不能同时进行,否则可能删掉刚复制、还没交给SDK的文件 */
    private val lock = Any()

    /** 复制文件的目录 */
    private val folder: File
        get() = File(DairoUI.context.filesDir, FOLDER)

    /**
     * 开始监听上传事件,SDK创建之后调用一次
     */
    fun start() {
        this.scope.launch {
            FileSdkUtil.events.collect {
                when (it) {
                    is Event.UploadStateChanged -> if (it.state == UploadState.FINISH || it.state == UploadState.SKIPPED) {
                        deleteUploadedFile(it.id)
                    }

                    is Event.UploadRemoved -> clean()
                    else -> {}
                }
            }
        }
        this.clean()
    }

    /**
     * 复制分享的文件并加入上传任务,不要在主线程调用
     * @param uris 分享的文件
     * @param folder 上传到的文件夹,空字符串表示根目录
     * @return 加入上传的文件数
     */
    fun save(uris: List<Uri>, folder: String): Int {
        synchronized(this.lock) {
            val batch = File(this.folder, UUID.randomUUID().toString())
            if (!batch.mkdirs()) {
                throw IllegalStateException("无法创建保存文件的目录")
            }
            try {
                val names = HashSet<String>()
                val items = ArrayList<UploadItem>()
                for (uri in uris) {
                    val name = uniqueName(fileName(uri), names)
                    val file = File(batch, name)
                    val input = try {
                        DairoUI.context.contentResolver.openInputStream(uri)
                    } catch (e: Exception) {
                        throw IllegalStateException("读取分享的文件失败:${e.message}")
                    } ?: throw IllegalStateException("读取分享的文件失败")
                    input.use { inStream ->
                        file.outputStream().use { inStream.copyTo(it, 1024 * 1024) }
                    }
                    names.add(name)
                    items.add(UploadItem(source = UploadSource.File(file.absolutePath), dfsPath = "$folder/$name"))
                }
                if (items.isEmpty()) {
                    throw IllegalStateException("没有可以上传的文件")
                }
                FileSdkUtil.get().addUploads(items)
                return items.size
            } catch (e: Throwable) {
                batch.deleteRecursively()
                throw e
            }
        }
    }

    /** 文件名,优先使用分享来源给的名字,没有扩展名时按文件类型补上 */
    private fun fileName(uri: Uri): String {
        var name = FileUploadSource.fileName(uri) ?: ""
        if (name.isEmpty()) {
            name = uri.lastPathSegment?.substringAfterLast('/') ?: ""
        }
        name = name.replace("/", "_").replace(":", "_").trim()
        if (name.isEmpty() || name.startsWith(".")) {
            name = "file$name"
        }
        if (!name.contains(".")) {
            val type = DairoUI.context.contentResolver.getType(uri)
            val ext = type?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }
            if (!ext.isNullOrEmpty()) {
                name += ".$ext"
            }
        }
        return name
    }

    /** 同一次分享里文件名重复时,在扩展名前面加序号,例如a(1).jpg */
    private fun uniqueName(name: String, exists: Set<String>): String {
        if (!exists.contains(name)) {
            return name
        }
        val dot = name.lastIndexOf('.')
        val base = if (dot > 0) name.substring(0, dot) else name
        val ext = if (dot > 0) name.substring(dot) else ""
        var index = 1
        while (true) {
            val newName = "$base($index)$ext"
            if (!exists.contains(newName)) {
                return newName
            }
            index += 1
        }
    }

    /** 复制的文件,不是分享的文件时返回null */
    private fun sharedFile(source: UploadSource): File? {
        if (source !is UploadSource.File) {
            return null
        }
        val file = File(source.path)
        return if (file.parentFile?.parentFile == this.folder) file else null
    }

    /** 上传完成之后删除复制的文件,目录空了之后一起删除 */
    private fun deleteUploadedFile(id: Long) {
        this.scope.launch(Dispatchers.IO) {
            val upload = runCatching { FileSdkUtil.get().getUpload(id) }.getOrNull() ?: return@launch
            val file = sharedFile(upload.source) ?: return@launch
            synchronized(lock) {
                file.delete()
                val batch = file.parentFile
                if (batch != null && batch.list().isNullOrEmpty()) {
                    batch.delete()
                }
            }
        }
    }

    /** 删除已经没有上传任务的文件,比如上传任务被用户删除了 */
    private fun clean() {
        this.scope.launch {
            withContext(Dispatchers.IO) {
                synchronized(lock) {
                    cleanNow()
                }
            }
        }
    }

    private fun cleanNow() {
        val batches = this.folder.listFiles()?.filter { it.isDirectory } ?: return
        if (batches.isEmpty()) {
            return
        }
        val sdk = runCatching { FileSdkUtil.get() }.getOrNull() ?: return

        //所有账号还没完成的上传任务在用的文件
        val usingFiles = HashSet<String>()
        val limit = 1000UL
        var offset = 0UL
        while (true) {
            val list = try {
                sdk.listUploads(UploadQuery(account = null, state = null, offset = offset, limit = limit))
            } catch (e: Throwable) {
                Log.e(TAG, "查询上传任务失败", e)
                return
            }
            for (upload in list) {
                if (upload.state == UploadState.FINISH || upload.state == UploadState.SKIPPED) {
                    continue
                }
                sharedFile(upload.source)?.let { usingFiles.add(it.absolutePath) }
            }
            if (list.size.toULong() < limit) {
                break
            }
            offset += limit
        }
        for (batch in batches) {
            batch.listFiles()?.forEach {
                if (!usingFiles.contains(it.absolutePath)) {
                    it.deleteRecursively()
                }
            }
            if (batch.list().isNullOrEmpty()) {
                batch.delete()
            }
        }
    }
}
