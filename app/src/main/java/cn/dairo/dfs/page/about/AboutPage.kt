package cn.dairo.dfs.page.about

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import cn.dairo.dfs.R
import cn.dairo.dfs.constants.Const
import cn.dairo.dfs.extension.primaryText
import cn.dairo.dfs.extension.secondaryText
import cn.dairo.ui.setting.SettingCustomView
import cn.dairo.ui.setting.SettingGroup
import cn.dairo.ui.setting.SettingNavigationLink
import cn.dairo.ui.setting.SettingPage
import cn.dairo.ui.setting.SettingStack
import cn.dairo.ui.setting.settingColors

/** 免责声明 */
private const val DISCLAIMER = "DairoDFS是一个文件管理客户端，本身不提供任何文件、图片、视频内容。App里浏览、上传和下载的内容全部保存在你自己填写的服务器上，由服务器的管理者负责。请只存储和分享你有权使用的内容，不要用于违法用途。"

/**
 * 关于,版本信息、免责声明、隐私政策
 */
@Composable
fun AboutPage() {
    Box(Modifier.fillMaxSize()) {
        SettingStack(embedInNavigationStack = false) {
            SettingPage(title = "关于") {
                SettingCustomView {
                    Column(
                        Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Image(painterResource(R.drawable.logo), contentDescription = null, modifier = Modifier.size(80.dp).clip(RoundedCornerShape(14.dp)))
                        Text("DairoDFS ${Const.versionName}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = Color.primaryText)
                    }
                }
                SettingGroup {
                    SettingNavigationLink("隐私政策") {
                        PrivacyPage()
                    }
                }
                SettingGroup(header = "免责声明") {
                    SettingCustomView {
                        Text(
                            DISCLAIMER,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.secondaryText,

                            //文字不够一行宽时,背景也要和其他分组一样宽
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                        )
                    }
                }
            }
        }

        //固定显示在屏幕最底部
        GithubLink(Modifier.align(Alignment.BottomCenter))
    }
}

/** 服务器端项目地址,点击用浏览器打开 */
@Composable
private fun GithubLink(modifier: Modifier) {
    val context = LocalContext.current
    Column(
        modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp).padding(bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("服务器端项目地址", style = MaterialTheme.typography.bodySmall, color = Color.secondaryText)
        Row(
            Modifier.clickable {
                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Const.SERVER_GITHUB_URL.toUri())) }
            },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Link, contentDescription = null, tint = settingColors.accent, modifier = Modifier.size(16.dp))
            Text(
                Const.SERVER_GITHUB_URL.replace("https://", ""),
                style = MaterialTheme.typography.bodySmall,
                color = settingColors.accent,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
    }
}
