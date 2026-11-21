package cn.dairo.dfs.page.fileuploadset

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Upload
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import cn.dairo.dfs.store.SettingStore
import cn.dairo.dfs.util.FileSdkUtil
import cn.dairo.ui.setting.SettingGroup
import cn.dairo.ui.setting.SettingIcon
import cn.dairo.ui.setting.SettingPage
import cn.dairo.ui.setting.SettingPicker
import cn.dairo.ui.setting.SettingPickerData
import cn.dairo.ui.setting.SettingStack

/** 同时上传文件数模版 */
private val UPLOAD_COUNT_DATA = (1..5).map { SettingPickerData("$it", it) }

@Composable
fun FileUploadSetPage() {

    /** 同时上传文件数 */
    var maxUploadingCount by remember { mutableIntStateOf(if (SettingStore.fileSdkMaxUploadingCount > 0) SettingStore.fileSdkMaxUploadingCount else 1) }
    SettingStack(embedInNavigationStack = false) {
        SettingPage(title = "上传设置") {
            SettingGroup {
                SettingPicker(
                    "同时上传文件数",
                    data = UPLOAD_COUNT_DATA,
                    value = maxUploadingCount,
                    onValueChange = {
                        maxUploadingCount = it
                        SettingStore.fileSdkMaxUploadingCount = it
                        runCatching { FileSdkUtil.get().setMaxUploadingCount(it.toUInt()) }
                    },
                    icon = SettingIcon.System(Icons.Default.Upload, backgroundColor = Color(0xFFFF3B30)),
                )
            }
        }
    }
}
