package cn.dairo.dfs.store

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import cn.dairo.dfs.api.CommonApi
import cn.dairo.dfs.api.LoginApi
import cn.dairo.dfs.api.MineApi
import cn.dairo.dfs.api.model.MineModel
import cn.dairo.dfs.components.relaunch
import cn.dairo.dfs.constants.Const
import cn.dairo.dfs.model.AccountInfo
import cn.dairo.dfs.page.login.LoginPage
import cn.dairo.dfs.page.login.LoginViewModel
import cn.dairo.dfs.util.FileSdkUtil
import cn.dairo.ui.DairoUI
import cn.dairo.ui.root.Toast
import cn.dairo.ui.util.FileUtil
import cn.dairo.ui.util.LocalObjectUtil
import java.net.URI
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * 用户设置
 */
object SettingStore {

    /** 设置保存的SharedPreferences,对应iOS的UserDefaults */
    private val preferences: SharedPreferences by lazy {
        DairoUI.context.getSharedPreferences("setting", Context.MODE_PRIVATE)
    }

    /*----------------------------------------------------------------------------------*/

    /** 会员信息 */
    private const val KEY_USER = "user"

    /** 会员信息 */
    var user: MineModel?
        get() = LocalObjectUtil.read<MineModel>(KEY_USER)
        set(value) {
            LocalObjectUtil.write(value, KEY_USER)
        }

    /*----------------------------------------------------------------------------------*/

    /** 登录Token */
    private const val KEY_TOKEN = "TOKEN"
    private var mToken: String? = null

    /** 登录Token */
    var token: String
        get() {
            mToken?.let { return it }
            val token = preferences.getString(KEY_TOKEN, null) ?: ""
            mToken = token
            return token
        }
        set(value) {
            mToken = value
            if (value.isEmpty()) {
                preferences.edit().remove(KEY_TOKEN).apply()
                return
            }
            preferences.edit().putString(KEY_TOKEN, value).apply()
        }

    /*----------------------------------------------------------------------------------*/

    /** 当前服务器域名 */
    private const val KEY_DOMAIN = "DOMAIN"
    private var mDomain: String? = null

    val domainNotNull: String
        get() = this.domain ?: ""

    /** 当前服务器是否局域网 */
    val isLanServer: Boolean
        get() = this.isLanDomain(this.domainNotNull)

    /**
     * 判断服务器地址是否局域网,只看主机名,不区分http和https
     * @param domain 服务器地址,例如:http://192.168.1.100:8031
     */
    fun isLanDomain(domain: String): Boolean {
        val host = runCatching { URI(domain).host?.lowercase() }.getOrNull() ?: return false
        if (host == "localhost" || host.endsWith(".local")) {
            return true
        }
        if (host.contains(":")) {//IPv6:本机、唯一本地地址(fc00::/7)、链路本地地址(fe80::/10)
            val ipv6 = host.trim('[', ']')
            return ipv6 == "::1"
                || ipv6.startsWith("fc") || ipv6.startsWith("fd")
                || ipv6.startsWith("fe8") || ipv6.startsWith("fe9") || ipv6.startsWith("fea") || ipv6.startsWith("feb")
        }

        //IPv4,要完整解析,否则192.168.example.com这样的域名也会被当成局域网
        val parts = host.split(".").map { it.toIntOrNull()?.takeIf { n -> n in 0..255 } }
        if (parts.size != 4 || parts.any { it == null }) {
            return false
        }
        val a = parts[0]!!
        val b = parts[1]!!
        return a == 10
            || a == 127
            || (a == 172 && b in 16..31)
            || (a == 192 && b == 168)
            || (a == 169 && b == 254)
    }

    var domain: String?
        get() {
            if (mDomain == null) {
                mDomain = preferences.getString(KEY_DOMAIN, null)
            }
            return mDomain
        }
        set(value) {
            mDomain = value
            if (value == null) {
                preferences.edit().remove(KEY_DOMAIN).apply()
                return
            }
            preferences.edit().putString(KEY_DOMAIN, value).apply()
        }

    /*----------------------------------------------------------------------------------*/

    /** 登录过的账号列表,保存的文件名 */
    private const val KEY_ACCOUNT_LIST = "account_list"

    /** 登录过的账号列表 */
    var accountList: List<AccountInfo>
        get() = LocalObjectUtil.read<List<AccountInfo>>(KEY_ACCOUNT_LIST) ?: emptyList()
        set(value) {
            LocalObjectUtil.write(value, KEY_ACCOUNT_LIST)
            mToken = null
        }

    /** 集群ID,同一个集群的服务器共用文件缓存 */
    var clusterId: String by StringPreference("CLUSTER_ID", "")

    /*----------------------------------------------------------------------------------*/

    /** 是否登录 */
    val isLogin: Boolean
        get() = this.token.isNotEmpty() && this.clusterId.isNotEmpty()

    /** 退出登录 */
    fun logout() {
        this.accountList = this.accountList.map {
            it.copy(token = if (it.isCurrent) "" else it.token, isCurrent = false)
        }
        this.token = ""
        FileSdkUtil.syncDfsAccount()
    }

    /** 获取当前账号的本地对象缓存名,不同账号的缓存互相隔离,没有账号信息时返回null,此时不使用缓存 */
    fun accountObjectName(name: String): String? {
        val account = FileSdkUtil.dfsAccount ?: return null
        val folder = "account_cache/" + account.replace(":", "_")
        FileUtil.createDirectory(LocalObjectUtil.resolvedFolder.absolutePath + "/" + folder)
        return "$folder/$name"
    }

    /** 退出登录,并跳转到登录页面,自动填写当前账号的信息 */
    fun toLoginPage() {
        val account = this.accountList.firstOrNull { it.isCurrent }
        this.logout()
        if (account != null) {
            relaunch { LoginPage(LoginViewModel(domain = account.domain, name = account.name, pwd = "")) }
        } else {
            relaunch { LoginPage() }
        }
    }

    /**
     * 登录
     * @param pwd 明文密码
     * @param editIndex 编辑账号时,账号在已登录列表里的序号,登录成功之后替换这个账号
     */
    fun login(domain: String, name: String, pwd: String, editIndex: Int? = null, success: () -> Unit) {

        //先记录登录之前的服务器，如果登录失败，则还原之前的服务器
        val oldDomain = this.domain
        val onFail = { msg: String ->
            this.domain = oldDomain
            Toast.show(msg)
        }
        this.domain = domain
        CommonApi.global().error(onFail).fail {
            onFail(it.msg ?: "获取服务器信息失败")
        }.send { global ->
            LoginApi.doLogin(name = name, pwd = pwd, deviceId = Const.deviceId).error(onFail).fail {
                onFail(it.msg ?: "登录失败")
            }.send { loginInfo ->//登录成功
                val account = AccountInfo(domain = domain, name = name, token = loginInfo.token, clusterId = global.clusterId, isCurrent = true)
                this.accountList = this.addAccount(account, editIndex)
                this.clusterId = global.clusterId
                this.token = loginInfo.token

                //加载用户信息,上传任务按用户区分,所以要等用户信息加载完成
                val onUserFail = { msg: String ->
                    this.logout()
                    Toast.show(msg)
                }
                MineApi.info().error(onUserFail).fail {
                    onUserFail(it.msg ?: "获取用户信息失败")
                }.send {
                    this.user = it
                    FileSdkUtil.syncDfsAccount()
                    FileSdkUtil.resumeFailedUploads()
                    success()
                }
            }
        }
    }

    /** 将登录成功的账号保存到已登录列表,同一个服务器的同一个用户只保留一条,编辑时保持账号原来的位置 */
    private fun addAccount(account: AccountInfo, editIndex: Int?): List<AccountInfo> {
        val list = this.accountList.map { it.copy(isCurrent = false) }.toMutableList()
        val isSameUser = { it: AccountInfo -> it.domain == account.domain && it.name == account.name }
        if (editIndex == null || editIndex !in list.indices) {
            list.removeAll(isSameUser)
            list.add(account)
            return list
        }
        list[editIndex] = account
        return list.filterIndexed { i, it -> i == editIndex || !isSameUser(it) }
    }

    /** 切换到已经登录过的账号,票据失效时会跳转到登录页面 */
    fun switchAccount(account: AccountInfo, success: () -> Unit) {
        val oldDomain = this.domain
        val oldToken = this.token
        val oldClusterId = this.clusterId
        val oldAccountList = this.accountList
        val onFail = { msg: String ->
            this.domain = oldDomain
            this.accountList = oldAccountList
            this.clusterId = oldClusterId
            this.token = oldToken
            Toast.show(msg)
        }
        this.domain = account.domain
        this.accountList = oldAccountList.map {
            it.copy(isCurrent = it.domain == account.domain && it.name == account.name)
        }
        this.clusterId = account.clusterId
        this.token = account.token
        MineApi.info().error(onFail).fail {
            onFail(it.msg ?: "切换账号失败")
        }.send {
            this.user = it
            FileSdkUtil.syncDfsAccount()
            success()
        }
    }

    /*----------------------------------------------------------------------------------*/

    /** 功能模式,值是FunctionMode,页面读取时自动刷新 */
    var functionMode: Int by IntPreference("FUNCTION_MODE", 0)

    /*----------------------------------------------------------------------------------*/

    /** 同时下载文件数,0表示没有设置过 */
    var fileSdkMaxSavingCount: Int by IntPreference("FILE_SDK_MAX_SAVING_COUNT", 0)

    /** 同时缓存文件数,0表示没有设置过 */
    var fileSdkMaxCachingCount: Int by IntPreference("FILE_SDK_MAX_CACHING_COUNT", 0)

    /** 缓存文件保存天数,null表示没有设置过,0表示永久 */
    var fileSdkCacheExpireDays: Int?
        get() = if (preferences.contains("FILE_SDK_CACHE_EXPIRE_DAYS")) preferences.getInt("FILE_SDK_CACHE_EXPIRE_DAYS", 0) else null
        set(value) {
            if (value == null) {
                preferences.edit().remove("FILE_SDK_CACHE_EXPIRE_DAYS").apply()
            } else {
                preferences.edit().putInt("FILE_SDK_CACHE_EXPIRE_DAYS", value).apply()
            }
        }

    /*----------------------------------------------------------------------------------*/

    /** 同时上传文件数,0表示没有设置过 */
    var fileSdkMaxUploadingCount: Int by IntPreference("FILE_SDK_MAX_UPLOADING_COUNT", 0)

    /*----------------------------------------------------------------------------------*/

    /** 记录最后打开的文件夹 */
    var lastOpenFolder: String by StringPreference("LAST_OPEN_FOLDER", "")

    /*----------------------------------------------------------------------------------*/

    /** 自动 */
    const val VIDEO_QUALITY_AUTO = 0

    /** 原画 */
    const val VIDEO_QUALITY_ORIGINAL = 1

    /** 流畅 */
    const val VIDEO_QUALITY_SMOOTH = 2

    /** 视频清晰度选择 */
    var videoQuality: Int by IntPreference("VIDEO_QUALITY", VIDEO_QUALITY_AUTO)

    /** 当前视频清晰度显示标题 */
    val videoQualityLabel: String
        get() = when (this.videoQuality) {
            VIDEO_QUALITY_ORIGINAL -> "原画"
            VIDEO_QUALITY_SMOOTH -> "流畅"
            else -> "自动"
        }

    /*----------------------------------------------------------------------------------*/

    /**
     * 保存在SharedPreferences里的设置,对应iOS页面上的@AppStorage
     * 值同时保存在Compose的状态里,页面读取之后,值变化时页面自动刷新
     */
    private abstract class Preference<T>(private val key: String, private val defaultValue: T) : ReadWriteProperty<Any?, T> {

        private var state: MutableState<T>? = null

        abstract fun get(preferences: SharedPreferences, key: String, defaultValue: T): T

        abstract fun put(editor: SharedPreferences.Editor, key: String, value: T)

        private fun state(): MutableState<T> {
            this.state?.let { return it }
            val state = mutableStateOf(this.get(preferences, this.key, this.defaultValue))
            this.state = state
            return state
        }

        override fun getValue(thisRef: Any?, property: KProperty<*>): T = this.state().value

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
            this.state().value = value
            val editor = preferences.edit()
            this.put(editor, this.key, value)
            editor.apply()
        }
    }

    private class IntPreference(key: String, defaultValue: Int) : Preference<Int>(key, defaultValue) {
        override fun get(preferences: SharedPreferences, key: String, defaultValue: Int) = preferences.getInt(key, defaultValue)
        override fun put(editor: SharedPreferences.Editor, key: String, value: Int) {
            editor.putInt(key, value)
        }
    }

    private class StringPreference(key: String, defaultValue: String) : Preference<String>(key, defaultValue) {
        override fun get(preferences: SharedPreferences, key: String, defaultValue: String) = preferences.getString(key, defaultValue) ?: defaultValue
        override fun put(editor: SharedPreferences.Editor, key: String, value: String) {
            editor.putString(key, value)
        }
    }
}
