package cn.dairo.dfs.page.file.view

import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DriveFolderUpload
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cn.dairo.dfs.api.FilesApi
import cn.dairo.dfs.components.AlertButton
import cn.dairo.dfs.components.ButtonRole
import cn.dairo.dfs.components.OptionButton
import cn.dairo.dfs.components.TextFieldAlert
import cn.dairo.dfs.page.file.viewmodel.FileViewModel
import cn.dairo.dfs.page.systemalbum.SystemAlbumPage
import cn.dairo.dfs.page.systemalbum.viewmodel.UploadMode
import cn.dairo.dfs.store.SettingStore
import cn.dairo.dfs.util.FileSdkUtil
import cn.dairo.dfs.util.FileUploadSource
import cn.dairo.dfs.util.fileSdkMessage
import cn.dairo.file.sdk.UploadItem
import cn.dairo.file.sdk.UploadSource
import cn.dairo.ui.DairoUI
import cn.dairo.ui.root.Toast
import cn.dairo.ui.setting.LocalSettingNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 添加文件的按钮:上传文件、上传文件夹、上传相册里的图片视频、创建文件夹
 */
object FileAddView {

    /** 文件添加视图打开状态广播通知标识 */
    const val FILE_ADD_VIEW_SHOW_FLAG = "FILE_ADD_VIEW_SHOW_FLAG"
}

@Composable
fun FileAddView(vm: FileViewModel) {
    val navigator = LocalSettingNavigator.current
    val scope = rememberCoroutineScope()

    /** 显示文件夹名称输入框 */
    var showCreateFolderAlert by remember { mutableStateOf(false) }

    /** 文件夹名称输入内容 */
    var createFolder by remember { mutableStateOf("") }

    //文件选择器,可以选多个文件
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isEmpty()) {
            return@rememberLauncherForActivityResult
        }
        val folder = SettingStore.lastOpenFolder
        scope.launch {
            upload {
                uris.map { uri ->
                    keepPermission(uri)
                    makeItem(uri, folder + "/" + (FileUploadSource.fileName(uri) ?: uri.lastPathSegment ?: "file"))
                }
            }
        }
    }

    //文件夹选择器
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { treeUri ->
        if (treeUri == null) {
            return@rememberLauncherForActivityResult
        }
        val folder = SettingStore.lastOpenFolder
        scope.launch {
            upload {
                keepPermission(treeUri)
                val rootId = DocumentsContract.getTreeDocumentId(treeUri)
                val rootUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, rootId)
                val name = FileUploadSource.fileName(rootUri) ?: "folder"
                makeFolderItems(treeUri, rootId, "$folder/$name")
            }
        }
    }

    if (!vm.isShowAddView) {
        return
    }
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        OptionButton("上传文件", Icons.Default.UploadFile) {
            filePicker.launch(arrayOf("*/*"))
        }
        OptionButton("上传文件夹", Icons.Default.DriveFolderUpload) {
            folderPicker.launch(null)
        }
        OptionButton("图片/视频", Icons.Default.AddPhotoAlternate) {
            navigator?.push { SystemAlbumPage(UploadMode.FILE) }
        }
        OptionButton("创建文件夹", Icons.Default.CreateNewFolder) {
            showCreateFolderAlert = true
        }
    }
    TextFieldAlert(
        isPresented = showCreateFolderAlert,
        title = "创建文件夹",
        text = createFolder,
        onTextChange = { createFolder = it },
        placeholder = "文件夹名",
        onDismiss = { showCreateFolderAlert = false },
        buttons = listOf(
            AlertButton("创建", ButtonRole.DESTRUCTIVE) {
                val name = createFolder

                //清空输入值,避免下次被显示
                createFolder = ""
                FilesApi.createFolder(SettingStore.lastOpenFolder + "/" + name).send {
                    vm.reload()
                }
            },
            AlertButton("取消", ButtonRole.CANCEL),
        ),
    )
}

/**
 * 添加上传任务,读取文件信息不在主线程
 * @param makeItems 生成上传任务列表
 */
private suspend fun upload(makeItems: () -> List<UploadItem>) {
    try {
        val items = withContext(Dispatchers.IO) { makeItems() }
        FileSdkUtil.get().addUploads(items)
    } catch (e: Throwable) {
        Toast.show(e.fileSdkMessage)
    }
}

/** 申请永久访问权限,App重启之后上传任务还能继续读取文件 */
private fun keepPermission(uri: Uri) {
    runCatching {
        DairoUI.context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
}

/** 一个上传任务 */
private fun makeItem(uri: Uri, dfsPath: String): UploadItem {
    return UploadItem(source = UploadSource.Custom(FileUploadSource.makeId(uri)), dfsPath = dfsPath)
}

/**
 * 生成文件夹里所有文件的上传任务,子文件夹递归
 * @param treeUri 选择的文件夹
 * @param documentId 当前文件夹
 * @param folder 当前文件夹上传到服务器的路径
 */
private fun makeFolderItems(treeUri: Uri, documentId: String, folder: String): List<UploadItem> {
    val items = ArrayList<UploadItem>()
    val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, documentId)
    val projection = arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_MIME_TYPE)
    DairoUI.context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
        while (cursor.moveToNext()) {
            val id = cursor.getString(0)
            val name = cursor.getString(1)

            //跳过隐藏文件
            if (name.startsWith(".")) {
                continue
            }
            if (cursor.getString(2) == DocumentsContract.Document.MIME_TYPE_DIR) {
                items.addAll(makeFolderItems(treeUri, id, "$folder/$name"))
            } else {
                items.add(makeItem(DocumentsContract.buildDocumentUriUsingTree(treeUri, id), "$folder/$name"))
            }
        }
    }
    return items
}
