package cn.dairo.dfs.util

import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import cn.dairo.file.sdk.CallbackException
import cn.dairo.file.sdk.UploadSourceProvider
import cn.dairo.ui.DairoUI
import cn.dairo.ui.util.toHex
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.security.MessageDigest

/**
 * 给SDK提供上传数据,支持系统相册和文件选择器选择的文件
 * SDK保存的是上传数据的ID,App重启之后会通过同一个ID重新读取数据,所以ID必须稳定
 * 相册文件的地址一直有效;文件选择器选择的文件在选择时已经申请了永久访问权限
 * 所有函数都在SDK的后台线程调用,可以阻塞
 */
class FileUploadSource : UploadSourceProvider {

    companion object {

        private const val URI_PREFIX = "uri:"

        /** 上传数据ID */
        fun makeId(uri: Uri): String {
            return URI_PREFIX + uri
        }

        /** 从上传数据ID里取出文件地址,格式不正确时返回null */
        fun uri(id: String): Uri? {
            if (!id.startsWith(URI_PREFIX)) {
                return null
            }
            return Uri.parse(id.removePrefix(URI_PREFIX))
        }

        /** 计算文件的MD5,不要在主线程调用 */
        fun md5(uri: Uri): String {
            val input = DairoUI.context.contentResolver.openInputStream(uri) ?: throw CallbackException.Failed("无法读取文件")
            val digest = MessageDigest.getInstance("MD5")
            input.use {
                val buffer = ByteArray(1024 * 1024)
                while (true) {
                    val count = it.read(buffer)
                    if (count < 0) {
                        break
                    }
                    digest.update(buffer, 0, count)
                }
            }
            return digest.digest().toHex()
        }

        /** 文件名,取不到时返回null */
        fun fileName(uri: Uri): String? {
            return runCatching {
                DairoUI.context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
                    if (it.moveToFirst()) it.getString(0) else null
                }
            }.getOrNull()
        }
    }

    /** 上传数据ID => 正在读取的数据 */
    private val sessions = HashMap<String, FileUploadSession>()

    /** 获取正在读取的数据 */
    private fun session(id: String): FileUploadSession {
        synchronized(this.sessions) {
            return this.sessions.getOrPut(id) { FileUploadSession(id) }
        }
    }

    override fun size(id: String): ULong {
        return this.session(id).size()
    }

    override fun read(id: String, offset: ULong, len: ULong): ByteArray {
        return this.session(id).read(offset.toLong(), len.toInt())
    }

    override fun close(id: String) {
        val session = synchronized(this.sessions) { this.sessions.remove(id) }
        session?.close()
    }
}

/**
 * 一个正在读取的上传数据,第一次读取时才打开
 */
private class FileUploadSession(private val id: String) {

    private var descriptor: ParcelFileDescriptor? = null

    private var channel: FileChannel? = null

    private var fileSize = 0L

    @Synchronized
    private fun openIfNeed(): FileChannel {
        this.channel?.let { return it }
        val uri = FileUploadSource.uri(this.id) ?: throw CallbackException.Failed("上传数据不正确")
        val descriptor = try {
            DairoUI.context.contentResolver.openFileDescriptor(uri, "r")
        } catch (e: Exception) {
            throw CallbackException.Failed("无法访问文件:${e.message}")
        } ?: throw CallbackException.Failed("无法访问文件")
        val channel = FileInputStream(descriptor.fileDescriptor).channel
        this.fileSize = if (descriptor.statSize >= 0) descriptor.statSize else channel.size()
        this.descriptor = descriptor
        this.channel = channel
        return channel
    }

    @Synchronized
    fun size(): ULong {
        this.openIfNeed()
        return this.fileSize.toULong()
    }

    /** 读取数据,除了文件末尾,返回的长度必须等于len */
    @Synchronized
    fun read(offset: Long, len: Int): ByteArray {
        val channel = this.openIfNeed()
        try {
            val buffer = ByteBuffer.allocate(len)
            var position = offset
            while (buffer.hasRemaining()) {
                val count = channel.read(buffer, position)
                if (count <= 0) {
                    break
                }
                position += count
            }
            return buffer.array().copyOf(buffer.position())
        } catch (e: Exception) {
            throw CallbackException.Failed("读取文件失败:${e.message}")
        }
    }

    @Synchronized
    fun close() {
        runCatching { this.channel?.close() }
        runCatching { this.descriptor?.close() }
        this.channel = null
        this.descriptor = null
    }
}
