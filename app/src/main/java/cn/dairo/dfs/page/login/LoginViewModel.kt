package cn.dairo.dfs.page.login

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cn.dairo.dfs.util.ObservableObject

class LoginViewModel(
    domain: String = "",
    name: String = "",
    pwd: String = "",

    /** 编辑模式时,当前账号所在的索引 */
    val editAccountIndex: Int? = null,
) : ObservableObject() {

    var domain by mutableStateOf(domain)

    var name by mutableStateOf(name)

    var pwd by mutableStateOf(pwd)
}
