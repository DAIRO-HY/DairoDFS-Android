package cn.dairo.dfs.page.file.entity

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cn.dairo.dfs.api.model.FileModel

class FileEntity(val fm: FileModel) {

    /** 是否选中 */
    var isSelected by mutableStateOf(false)
}
