package cn.dairo.dfs.page.mine

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cn.dairo.dfs.api.MineApi
import cn.dairo.dfs.model.AccountInfo
import cn.dairo.dfs.store.SettingStore
import cn.dairo.dfs.util.ObservableObject

class MineViewModel : ObservableObject() {

    /** 当前登录的账号信息 */
    val logged: AccountInfo? = SettingStore.accountList.firstOrNull { it.isCurrent }

    /** 当前登录的用户信息 */
    var user by mutableStateOf(SettingStore.user)

    /** 刷新用户信息,昵称和头像可能在个人资料页或其他设备上修改过 */
    fun loadUser() {
        this.user = SettingStore.user
        MineApi.info().hide().send {
            SettingStore.user = it
            this.user = it
        }
    }
}
