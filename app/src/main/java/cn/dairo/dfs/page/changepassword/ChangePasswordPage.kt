package cn.dairo.dfs.page.changepassword

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.input.KeyboardType
import cn.dairo.dfs.page.login.LogoView
import cn.dairo.dfs.util.rememberStateObject
import cn.dairo.ui.setting.SettingButtonSingle
import cn.dairo.ui.setting.SettingCustomView
import cn.dairo.ui.setting.SettingGroup
import cn.dairo.ui.setting.SettingPage
import cn.dairo.ui.setting.SettingStack
import cn.dairo.ui.setting.SettingTextField
import cn.dairo.ui.setting.SettingTextFieldType

@Composable
fun ChangePasswordPage() {

    //密码信息
    val vm = rememberStateObject { ChangePasswordViewModel() }
    SettingStack(embedInNavigationStack = false) {
        SettingPage(title = "密码修改") {
            SettingCustomView {
                LogoView()
            }
            SettingGroup {
                SettingTextField(title = "旧密码", text = vm.oldPwd, onTextChange = { vm.oldPwd = it }, placeholder = "请输入当前密码", type = SettingTextFieldType.PASSWORD, keyboardType = KeyboardType.Password)
                SettingTextField(title = "新密码", text = vm.pwd, onTextChange = { vm.pwd = it }, placeholder = "请输入新密码", type = SettingTextFieldType.PASSWORD, keyboardType = KeyboardType.Password)
                SettingTextField(title = "确认密码", text = vm.repwd, onTextChange = { vm.repwd = it }, placeholder = "请确认新密码", type = SettingTextFieldType.PASSWORD, keyboardType = KeyboardType.Password)
            }
            SettingGroup {
                SettingButtonSingle("修改", onClick = vm::onModifyClick)
            }
        }
    }
}
