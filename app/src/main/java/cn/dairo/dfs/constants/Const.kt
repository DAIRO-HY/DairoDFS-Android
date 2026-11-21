package cn.dairo.dfs.constants

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import cn.dairo.dfs.BuildConfig
import cn.dairo.ui.DairoUI
import cn.dairo.ui.util.md5
import java.util.UUID

/**
 * 静态变量配置
 */
object Const {

    /** 服务器端的GitHub项目地址 */
    const val SERVER_GITHUB_URL = "https://github.com/DAIRO-HY/DairoDFS"

    /** 本App的GitHub仓库,从这里的Release检查更新 */
    const val APP_GITHUB_REPO = "DAIRO-HY/DairoDFS-Android"

    /** 设备标识保存的名字 */
    private const val KEY_DEVICE_ID = "DEVICE_ID"

    private var mDeviceId: String? = null

    /**
     * 获取设备唯一标识
     * 用系统的ANDROID_ID(同一台设备、同一个签名的App不变)计算,取不到时随机生成并保存
     */
    val deviceId: String
        @SuppressLint("HardwareIds")
        get() {
            mDeviceId?.let { return it }
            val preferences = DairoUI.context.getSharedPreferences("setting", Context.MODE_PRIVATE)
            preferences.getString(KEY_DEVICE_ID, null)?.let {
                mDeviceId = it
                return it
            }
            val androidId = Settings.Secure.getString(DairoUI.context.contentResolver, Settings.Secure.ANDROID_ID)
            val deviceId = if (androidId.isNullOrEmpty()) UUID.randomUUID().toString().md5 else ("DairoDFS_$androidId").md5
            preferences.edit().putString(KEY_DEVICE_ID, deviceId).apply()
            mDeviceId = deviceId
            return deviceId
        }

    /** 整数版本号 */
    val versionCode: Int
        get() = BuildConfig.VERSION_CODE

    /** 版本名 */
    val versionName: String
        get() = BuildConfig.VERSION_NAME
}
