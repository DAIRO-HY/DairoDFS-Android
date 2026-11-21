package cn.dairo.dfs.page.album.entity

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cn.dairo.dfs.api.model.FileModel

class AlbumEntity(

    /** 当前序号 */
    val index: Int,

    val fm: FileModel,
) {

    /** 是否选中 */
    var isSelected by mutableStateOf(false)
}
