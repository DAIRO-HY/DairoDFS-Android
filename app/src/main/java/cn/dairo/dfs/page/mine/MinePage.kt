package cn.dairo.dfs.page.mine

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cn.dairo.dfs.api.LoginApi
import cn.dairo.dfs.components.OnPageAppear
import cn.dairo.dfs.components.UserAvatar
import cn.dairo.dfs.components.relaunch
import cn.dairo.dfs.constants.FunctionMode
import cn.dairo.dfs.extension.groupedBackground
import cn.dairo.dfs.extension.primaryText
import cn.dairo.dfs.extension.secondaryText
import cn.dairo.dfs.page.about.AboutPage
import cn.dairo.dfs.page.accountlist.AccountListPage
import cn.dairo.dfs.page.album.AlbumPage
import cn.dairo.dfs.page.changepassword.ChangePasswordPage
import cn.dairo.dfs.page.file.FilePage
import cn.dairo.dfs.page.filedownload.FileDownloadPage
import cn.dairo.dfs.page.fileupload.FileUploadPage
import cn.dairo.dfs.page.hometabview.HomeTabView
import cn.dairo.dfs.page.hometabview.TabPage
import cn.dairo.dfs.page.login.LoginPage
import cn.dairo.dfs.page.profile.ProfilePage
import cn.dairo.dfs.page.trash.TrashPage
import cn.dairo.dfs.store.SettingStore
import cn.dairo.dfs.util.rememberStateObject
import cn.dairo.ui.setting.LocalSettingNavigator
import cn.dairo.ui.setting.SettingButtonSingle
import cn.dairo.ui.setting.SettingCustomView
import cn.dairo.ui.setting.SettingGroup
import cn.dairo.ui.setting.SettingIcon
import cn.dairo.ui.setting.SettingNavigationLink
import cn.dairo.ui.setting.SettingPage
import cn.dairo.ui.setting.SettingPicker
import cn.dairo.ui.setting.SettingPickerData
import cn.dairo.ui.setting.SettingStack
import cn.dairo.ui.setting.settingRowClickable
import cn.dairo.ui.theme.ThemeMode
import cn.dairo.ui.theme.rememberThemeMode

/** 主题选择数据 */
private val THEME_DATA = listOf(
    SettingPickerData("跟随系统", ThemeMode.SYSTEM.value),
    SettingPickerData("明亮模式", ThemeMode.LIGHT.value),
    SettingPickerData("黑暗模式", ThemeMode.DARK.value),
)

/** 功能模式数据 */
private val FUNCTION_DATA = listOf(
    SettingPickerData("文件模式", FunctionMode.FILE),
    SettingPickerData("相册模式", FunctionMode.ALBUM),
)

/** 设置项的图标 */
private fun icon(icon: ImageVector, color: Long) = SettingIcon.System(icon, backgroundColor = Color(color))

@Composable
fun MinePage() {
    val vm = rememberStateObject { MineViewModel() }
    val context = LocalContext.current
    val themeMode by rememberThemeMode()
    SettingStack {
        OnPageAppear {
            vm.loadUser()
        }
        Column(Modifier.fillMaxSize().background(Color.groupedBackground)) {

            //底部的Tab栏已经留出了导航栏的高度,页面里不用再留
            Box(Modifier.weight(1f).statusBarsPadding().consumeWindowInsets(WindowInsets.navigationBars)) {
                SettingPage(showsTopBar = false, verticalPadding = 16.dp) {
                    SettingGroup {
                        SettingCustomView {
                            TopLogoView(vm)
                        }
                    }
                    SettingGroup {
                        SettingNavigationLink("回收站", tip = "已删除的文件", icon = icon(Icons.Default.Delete, 0xFF34C759)) {
                            TrashPage()
                        }
                        SettingNavigationLink("上传管理", tip = "上传文件列表", icon = icon(Icons.Default.Upload, 0xFF32ADE6)) {
                            FileUploadPage()
                        }
                        SettingNavigationLink("下载管理", tip = "下载、缓存文件", icon = icon(Icons.Default.Download, 0xFF5856D6)) {
                            FileDownloadPage()
                        }
                    }
                    SettingGroup {
                        SettingPicker(
                            "切换主题",
                            data = THEME_DATA,
                            value = themeMode.value,
                            onValueChange = { ThemeMode.set(context, ThemeMode.of(it)) },
                            icon = icon(Icons.Default.Brush, 0xFFFF3B30),
                        )
                        SettingPicker(
                            "功能模式",
                            data = FUNCTION_DATA,
                            value = SettingStore.functionMode,
                            onValueChange = { SettingStore.functionMode = it },

                            //选中当前值时也会调用,从相册页退出后再选相册模式要能回到相册页
                            shouldChange = {
                                if (it == FunctionMode.ALBUM) {
                                    relaunch { AlbumPage() }
                                } else {
                                    relaunch { FilePage() }
                                }
                                true
                            },
                            icon = icon(Icons.Default.Casino, 0xFFAF52DE),
                        )
                    }
                    SettingGroup {
                        SettingNavigationLink("个人资料", tip = "昵称、头像", icon = icon(Icons.Default.AccountCircle, 0xFF007AFF)) {
                            ProfilePage()
                        }
                        SettingNavigationLink("修改密码", icon = icon(Icons.Default.LockReset, 0xFFFF9500)) {
                            ChangePasswordPage()
                        }
                    }
                    SettingGroup {
                        SettingNavigationLink("关于", icon = icon(Icons.Default.Info, 0xFF8E8E93)) {
                            AboutPage()
                        }
                    }
                    SettingGroup {
                        SettingButtonSingle("退出登录") {

                            //让服务端删除这个票据
                            LoginApi.logout().hide().send {}
                            SettingStore.logout()
                            relaunch { LoginPage() }
                        }
                    }
                }
            }
            HomeTabView(TabPage.MINE_PAGE)
        }
    }
}

/** 顶部头像部分 */
@Composable
private fun TopLogoView(vm: MineViewModel) {
    val navigator = LocalSettingNavigator.current
    Row(
        Modifier
            .fillMaxWidth()
            .settingRowClickable { navigator?.push { AccountListPage() } }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        UserAvatar(vm.user, 50.dp)
        Column(Modifier.weight(1f).padding(start = 14.dp)) {
            Text(
                vm.user?.displayName ?: vm.logged?.name ?: "",
                style = MaterialTheme.typography.titleLarge,
                color = Color.primaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(vm.logged?.domain ?: "", style = MaterialTheme.typography.bodySmall, color = Color.secondaryText, modifier = Modifier.padding(top = 2.dp))
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.secondaryText)
    }
}
