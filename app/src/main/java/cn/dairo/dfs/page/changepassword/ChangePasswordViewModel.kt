package cn.dairo.dfs.page.changepassword

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cn.dairo.dfs.api.ModifyApi
import cn.dairo.dfs.store.SettingStore
import cn.dairo.dfs.util.ObservableObject
import cn.dairo.ui.root.Toast

class ChangePasswordViewModel : ObservableObject() {

    /** 旧密码 */
    var oldPwd by mutableStateOf("")

    /** 新密码 */
    var pwd by mutableStateOf("")

    /** 确认新密码 */
    var repwd by mutableStateOf("")

    /** 修改按钮点击事件 */
    fun onModifyClick() {
        if (this.oldPwd.isEmpty()) {
            Toast.show("请输入旧密码")
            return
        }
        if (this.pwd.isEmpty()) {
            Toast.show("请输入新密码")
            return
        }
        if (this.pwd != this.repwd) {
            Toast.show("确认密码和新密码不一致")
            return
        }
        ModifyApi.modify(oldPwd = this.oldPwd, pwd = this.pwd).send {
            Toast.show("修改成功,请重新登录")
            SettingStore.toLoginPage()
        }
    }
}
