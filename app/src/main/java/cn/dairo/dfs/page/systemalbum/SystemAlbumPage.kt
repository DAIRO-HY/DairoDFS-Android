package cn.dairo.dfs.page.systemalbum

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import cn.dairo.dfs.components.NavigationPage
import cn.dairo.dfs.extension.secondaryText
import cn.dairo.dfs.page.album.view.albumColumnCount
import cn.dairo.dfs.page.systemalbum.view.SystemAlbumImageView
import cn.dairo.dfs.page.systemalbum.view.SystemAlbumOptionView
import cn.dairo.dfs.page.systemalbum.viewmodel.SystemAlbumViewModel
import cn.dairo.dfs.page.systemalbum.viewmodel.UploadMode
import cn.dairo.dfs.util.OnFileSdkEvent
import cn.dairo.dfs.util.rememberStateObject
import cn.dairo.ui.DairoUI
import cn.dairo.ui.root.Toast
import cn.dairo.ui.setting.settingRowClickable

/** 表格间距 */
private val SPACING = 2.dp

/** 读取相册需要的权限,Android 14可以只授权部分照片 */
private val READ_PERMISSIONS = when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
    else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
}

/** 是否有读取相册的权限,部分照片的权限也算 */
private fun hasReadPermission(): Boolean {
    return READ_PERMISSIONS.any { ContextCompat.checkSelfPermission(DairoUI.context, it) == PackageManager.PERMISSION_GRANTED }
}

/**
 * 上传系统相册里的照片和视频
 */
@Composable
fun SystemAlbumPage(uploadMode: UploadMode) {
    val vm = rememberStateObject { SystemAlbumViewModel(uploadMode) }
    OnFileSdkEvent(vm::onFileSdkEvent)
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (hasReadPermission()) {
            vm.fetchPhotos()
        } else {
            vm.isDenied = true
            Toast.show("未授权访问相册")
        }
    }

    //Android 11开始删除其他App的照片要用户确认
    val deleteLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
            vm.fetchPhotos()
            Toast.show("照片已删除")
        }
    }
    LaunchedEffect(Unit) {
        if (hasReadPermission()) {
            vm.fetchPhotos()
        } else {
            permissionLauncher.launch(READ_PERMISSIONS)
        }
    }
    val onDeleteClick: () -> Unit = {
        val uris = vm.uploadedUris
        if (uris.isEmpty()) {
            Toast.show("没有检查到已上传的文件")
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val request = MediaStore.createDeleteRequest(DairoUI.context.contentResolver, uris)
            deleteLauncher.launch(IntentSenderRequest.Builder(request.intentSender).build())
        } else {
            vm.deleteUploaded(uris)
        }
    }
    NavigationPage(title = "所有照片") {
        Box(Modifier.fillMaxWidth().weight(1f)) {
            if (vm.isDenied) {
                Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("未授权访问相册", color = Color.secondaryText)
                    TextButton(onClick = { permissionLauncher.launch(READ_PERMISSIONS) }) {
                        Text("重新授权")
                    }
                }
            } else {
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    val columnNum = albumColumnCount(maxWidth)

                    //每列宽度 = (屏幕宽度 - (列数 - 1) * 列间距) / 列数
                    val width = (maxWidth - SPACING * (columnNum - 1)) / columnNum
                    val state = rememberLazyGridState()

                    //打开时滚动到最底部,显示最新的照片
                    LaunchedEffect(vm.albumList) {
                        if (vm.albumList.isNotEmpty()) {
                            state.scrollToItem(vm.albumList.size - 1)
                        }
                    }
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(columnNum),
                        state = state,
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(SPACING),
                        verticalArrangement = Arrangement.spacedBy(SPACING),

                        //操作按钮浮在照片上面
                        contentPadding = PaddingValues(bottom = 140.dp),
                    ) {
                        items(vm.albumList, key = { it.identifier }) { item ->
                            SystemAlbumImageView(item, width, Modifier.settingRowClickable { vm.onItemClick(item) })
                        }
                    }
                }
            }
            SystemAlbumOptionView(vm, onDeleteClick, Modifier.align(Alignment.BottomCenter))
        }
    }
}
