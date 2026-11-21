package cn.dairo.dfs.page.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cn.dairo.dfs.components.NavigationPage
import cn.dairo.dfs.extension.primaryText
import cn.dairo.dfs.extension.secondaryText

/** 生效日期 */
private const val EFFECTIVE_DATE = "2026年11月7日"

/**
 * 每一节的标题和内容
 * 和iOS版、Document/PrivacyPolicy.md的中文部分一致,只有权限一节按Android的权限调整,修改时几处一起改
 */
private val SECTIONS = listOf(
    "说明" to """
        DairoDFS（以下简称"本App"）是一款开源的文件管理客户端，用来连接你自己部署的DairoDFS服务器。本政策说明本App会处理哪些信息，以及这些信息去了哪里。
    """.trimIndent(),
    "我们不收集你的任何信息" to """
        本App的开发者不运营任何收集用户数据的服务器。本App：
        · 不收集、不上传、不出售任何个人信息给开发者或第三方；
        · 不包含广告，不包含任何第三方统计、分析或追踪SDK；
        · 不追踪你在其他App或网站上的活动。
    """.trimIndent(),
    "你的数据只发送到你自己指定的服务器" to """
        使用本App时，你需要填写服务器地址、用户名和密码。本App只会和你填写的这台服务器通信，传输的内容包括：
        · 登录时的用户名和密码，用来换取登录票据；
        · 你浏览、上传、下载、移动、删除的文件和文件夹；
        · 你主动选择上传的照片和视频；
        · 一个由设备标识计算出的设备ID，供服务器区分登录设备。
        这台服务器由你自己或你的服务器管理员部署和管理，数据如何存储和保护，由服务器的管理者负责。本App的开发者无法访问这些数据。
    """.trimIndent(),
    "保存在你设备上的信息" to """
        为了让App正常使用，以下信息只保存在你的设备上：
        · 服务器地址、用户名和登录票据（不保存密码）；
        · 主题、功能模式、下载数量等设置；
        · 已浏览过的图片、视频、音乐等文件的缓存，以及你主动下载的文件。
        你可以在App的"下载管理"中清理缓存。删除本App会同时删除以上所有信息。
    """.trimIndent(),
    "App使用的权限" to """
        · 照片和视频：用来显示系统相册里的照片和视频，把你选择的内容上传到你的服务器，并在你确认后删除已上传的照片。
        · 存储（Android 9及以下）：用来把服务器上的照片和视频保存到系统相册。
        · 通知：上传、下载文件时在通知栏显示进度，播放音频时显示播放控制。
        以上权限都只在你使用对应功能时才会请求，你可以随时在系统"设置"中关闭。
    """.trimIndent(),
    "儿童隐私" to """
        本App不面向儿童收集任何信息。
    """.trimIndent(),
    "政策更新" to """
        如果本政策有变化，我们会更新本页面并修改上方的生效日期。
    """.trimIndent(),
    "联系我们" to """
        如果你对本政策有任何疑问，请发送邮件到 hy.zhoulq@gmail.com，或在GitHub上提交Issue：https://github.com/DAIRO-HY/DairoDFS-Android/issues
    """.trimIndent(),
)

/**
 * 隐私政策
 */
@Composable
fun PrivacyPage() {
    NavigationPage(title = "隐私政策") {
        Box(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier.widthIn(max = 700.dp).fillMaxWidth().padding(16.dp).navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Text("生效日期：$EFFECTIVE_DATE", style = MaterialTheme.typography.bodySmall, color = Color.secondaryText)
                SECTIONS.forEach { (title, content) ->
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = Color.primaryText)
                        Text(content, style = MaterialTheme.typography.bodyMedium, color = Color.secondaryText)
                    }
                }
            }
        }
    }
}
