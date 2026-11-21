package cn.dairo.dfs.page.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cn.dairo.dfs.components.ActionSheet
import cn.dairo.dfs.components.AlertButton
import cn.dairo.dfs.components.ButtonRole
import cn.dairo.dfs.components.UserAvatar
import cn.dairo.dfs.extension.secondaryText
import cn.dairo.dfs.util.rememberStateObject
import cn.dairo.ui.image.rememberSystemImagePicker
import cn.dairo.ui.root.Toast
import cn.dairo.ui.setting.SettingButtonSingle
import cn.dairo.ui.setting.SettingCustomView
import cn.dairo.ui.setting.SettingGroup
import cn.dairo.ui.setting.SettingPage
import cn.dairo.ui.setting.SettingStack
import cn.dairo.ui.setting.SettingTextField

@Composable
fun ProfilePage() {
    val vm = rememberStateObject { ProfileViewModel() }

    /** 显示头像操作菜单 */
    var showAvatarMenu by remember { mutableStateOf(false) }

    /** 系统图片选择器,只能选一张,不需要相册权限 */
    val pickImage = rememberSystemImagePicker(maxSize = ProfileViewModel.AVATAR_MAX_SIZE) { bitmap ->
        if (bitmap != null) {
            vm.onAvatarPick(bitmap)
        } else {
            Toast.show("读取图片失败")
        }
    }
    SettingStack(embedInNavigationStack = false) {
        SettingPage(title = "个人资料") {
            SettingCustomView {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(
                        Modifier.clip(RoundedCornerShape(12.dp)).clickable { showAvatarMenu = true }.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        UserAvatar(vm.user, 80.dp)
                        Text("更换头像", style = MaterialTheme.typography.bodySmall, color = Color.secondaryText)
                    }
                }
            }
            SettingGroup {
                SettingTextField(title = "昵称", text = vm.nickname, onTextChange = { vm.nickname = it }, placeholder = vm.user?.name ?: "")
            }
            SettingGroup {
                SettingButtonSingle("保存昵称", onClick = vm::onSaveNicknameClick)
            }
        }
    }
    ActionSheet(
        isPresented = showAvatarMenu,
        title = "更换头像",
        onDismiss = { showAvatarMenu = false },
        buttons = buildList {
            add(AlertButton("从相册选择", action = pickImage))
            if (vm.hasAvatar) {
                add(AlertButton("删除头像", ButtonRole.DESTRUCTIVE, vm::onDeleteAvatarClick))
            }
            add(AlertButton("取消", ButtonRole.CANCEL))
        },
    )
}
