package cn.dairo.dfs.page.accountlist

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cn.dairo.dfs.model.AccountInfo
import cn.dairo.dfs.store.SettingStore
import cn.dairo.dfs.util.ObservableObject

class AccountListViewModel : ObservableObject() {

    /** 记录当前要操作的索引 */
    var actionIndex = -1

    /** 当前已登录的账号列表 */
    var accountList by mutableStateOf(emptyList<AccountInfo>())

    /** 获取当前操作的账号 */
    val actionItem: AccountInfo
        get() = this.accountList[this.actionIndex]

    init {
        this.loadData()
    }

    /** 加载数据 */
    fun loadData() {
        this.accountList = SettingStore.accountList
    }

    /** 删除账号点击事件 */
    fun onDeleteClick() {
        val actionItem = this.actionItem
        val index = this.accountList.indexOfFirst { it.domain == actionItem.domain && it.name == actionItem.name }
        if (index >= 0) {
            this.accountList = this.accountList.filterIndexed { i, _ -> i != index }
        }
        SettingStore.accountList = this.accountList
    }
}
