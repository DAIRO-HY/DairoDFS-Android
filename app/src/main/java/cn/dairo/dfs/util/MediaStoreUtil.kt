package cn.dairo.dfs.util

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import cn.dairo.ui.DairoUI
import java.io.File
import java.io.InputStream

/**
 * 系统相册的读写,对应iOS的PHPhotoLibrary
 */
object MediaStoreUtil {

    /** 保存到相册里的文件夹名 */
    private const val FOLDER = "DairoDFS"

    /**
     * Android 9及以下保存到相册需要存储权限,没有权限时申请,返回false
     */
    fun checkWritePermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return true
        }
        val context = DairoUI.context
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            return true
        }
        AppActivity.current?.let {
            ActivityCompat.requestPermissions(it, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 0)
        }
        return false
    }

    /**
     * 保存图片或视频到系统相册,不能在主线程调用
     * @param name 文件名,决定相册里的文件格式
     * @param isVideo 是否视频
     * @param open 打开要保存的数据
     */
    fun save(name: String, isVideo: Boolean, open: () -> InputStream) {
        val resolver = DairoUI.context.contentResolver
        val ext = name.substringAfterLast('.', "").lowercase()
        val mimeType = android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: if (isVideo) "video/mp4" else "image/jpeg"
        val collection: Uri = if (isVideo) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY) else MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY) else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, (if (isVideo) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_PICTURES) + "/" + FOLDER)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            } else {

                //Android 9及以下要指定文件的完整路径
                @Suppress("DEPRECATION")
                val folder = File(Environment.getExternalStoragePublicDirectory(if (isVideo) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_PICTURES), FOLDER)
                folder.mkdirs()
                @Suppress("DEPRECATION")
                put(MediaStore.MediaColumns.DATA, File(folder, name).absolutePath)
            }
        }
        val uri = resolver.insert(collection, values) ?: throw IllegalStateException("无法创建相册文件")
        try {
            resolver.openOutputStream(uri)?.use { output ->
                open().use { it.copyTo(output) }
            } ?: throw IllegalStateException("无法写入相册文件")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
            }
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            throw e
        }
    }
}
