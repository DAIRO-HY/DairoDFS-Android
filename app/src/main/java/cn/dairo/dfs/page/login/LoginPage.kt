package cn.dairo.dfs.page.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import cn.dairo.dfs.R
import cn.dairo.dfs.components.relaunch
import cn.dairo.dfs.extension.secondaryText
import cn.dairo.dfs.page.accountlist.AccountListPage
import cn.dairo.dfs.page.mine.MinePage
import cn.dairo.dfs.store.SettingStore
import cn.dairo.dfs.util.rememberStateObject
import cn.dairo.ui.root.Toast
import cn.dairo.ui.setting.SettingButtonSingle
import cn.dairo.ui.setting.SettingCustomView
import cn.dairo.ui.setting.SettingGroup
import cn.dairo.ui.setting.SettingPage
import cn.dairo.ui.setting.SettingStack
import cn.dairo.ui.setting.SettingTextField
import cn.dairo.ui.setting.SettingTextFieldType

@Composable
fun LoginPage(loginViewModel: LoginViewModel? = null) {

    //登录信息,页面存在期间只创建一次,保留已输入的内容
    val loginInfo = rememberStateObject { loginViewModel ?: LoginViewModel() }
    SettingStack {
        SettingPage(title = if (loginInfo.editAccountIndex == null) "添加账号" else "编辑账号") {
            SettingCustomView {
                LogoView()
            }
            SettingGroup {
                SettingTextField(title = "服务器", text = loginInfo.domain, onTextChange = { loginInfo.domain = it }, placeholder = "例 192.168.1.100:8031", keyboardType = KeyboardType.Uri)
                SettingTextField(title = "用户名", text = loginInfo.name, onTextChange = { loginInfo.name = it }, placeholder = "请输入用户名", keyboardType = KeyboardType.Email)
                SettingTextField(title = "密码", text = loginInfo.pwd, onTextChange = { loginInfo.pwd = it }, placeholder = "请输入密码", type = SettingTextFieldType.PASSWORD, keyboardType = KeyboardType.Password)
            }
            SettingGroup {
                SettingButtonSingle(if (loginInfo.editAccountIndex == null) "登录" else "保存") {
                    onLoginClick(loginInfo)
                }
            }
            SettingCustomView {
                Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { relaunch { AccountListPage(embedInNavigationStack = true) } }) {
                        Text("选择账号", style = MaterialTheme.typography.bodyMedium, color = Color.secondaryText)
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { Toast.show("忘记密码请联系管理员") }) {
                        Text("忘记密码", style = MaterialTheme.typography.bodyMedium, color = Color.secondaryText)
                    }
                }
            }
        }
    }
}

/** 页面顶部的App图标 */
@Composable
fun LogoView() {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Image(
            painterResource(R.drawable.logo),
            contentDescription = null,
            modifier = Modifier.padding(32.dp).size(80.dp).clip(RoundedCornerShape(14.dp)),
        )
    }
}

/**
 * 登录/保存按钮点击事件
 */
private fun onLoginClick(loginInfo: LoginViewModel) {
    var domain = loginInfo.domain.trim()
    if (domain.isEmpty()) {
        Toast.show("服务器必填")
        return
    }
    if (!domain.startsWith("http://") && !domain.startsWith("https://")) {

        //补全url
        domain = "http://$domain"
    }

    //去掉最后一个/
    domain = domain.removeSuffix("/")

    //用户名
    if (loginInfo.name.isEmpty()) {
        Toast.show("用户名必填")
        return
    }

    //登录密码
    val pwd = loginInfo.pwd
    if (pwd.isEmpty()) {
        Toast.show("密码必填")
        return
    }
    SettingStore.login(domain = domain, name = loginInfo.name, pwd = pwd, editIndex = loginInfo.editAccountIndex) {
        relaunch { MinePage() }
    }
}
