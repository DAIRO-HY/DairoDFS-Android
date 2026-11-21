package cn.dairo.dfs.page.trash.entity

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cn.dairo.dfs.api.model.TrashModel

class TrashEntity(val fm: TrashModel) {

    /** 是否选中 */
    var isSelected by mutableStateOf(false)
}
