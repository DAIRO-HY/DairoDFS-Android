package cn.dairo.dfs.page.trash.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cn.dairo.dfs.api.TrashApi
import cn.dairo.dfs.page.trash.entity.TrashEntity
import cn.dairo.dfs.util.ObservableObject
import cn.dairo.ui.root.Toast

class TrashViewModel : ObservableObject() {

    /** 记录当前选中的文件数 */
    var selectedCount by mutableIntStateOf(0)

    /** 回收站数据列表 */
    var entities by mutableStateOf(emptyList<TrashEntity>())

    init {
        this.loadData()
    }

    /** 加载数据 */
    fun loadData() {
        TrashApi.getList().send { list ->
            this.entities = list.map { TrashEntity(it) }
            this.clearSelected()
        }
    }

    /** 选中状态点击事件 */
    fun onItemClick(item: TrashEntity) {
        item.isSelected = !item.isSelected
        this.selectedCount += if (item.isSelected) 1 else -1
    }

    /** 清空已经选择的文件 */
    fun clearSelected() {
        this.entities.forEach { it.isSelected = false }
        this.selectedCount = 0
    }

    /** 全选 */
    fun selectAll() {
        this.entities.forEach { it.isSelected = true }
        this.selectedCount = this.entities.size
    }

    /** 删除按钮点击事件 */
    fun onDeleteClick() {
        TrashApi.delete(this.entities.filter { it.isSelected }.map { it.fm.id }).send {
            Toast.show("操作成功")
            this.loadData()
        }
    }

    /** 还原按钮点击事件 */
    fun onRecoverClick() {
        TrashApi.recover(this.entities.filter { it.isSelected }.map { it.fm.id }).send {
            Toast.show("操作成功")
            this.loadData()
        }
    }
}
