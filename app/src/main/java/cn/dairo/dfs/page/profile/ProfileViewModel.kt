package cn.dairo.dfs.page.profile

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cn.dairo.dfs.api.MineApi
import cn.dairo.dfs.store.SettingStore
import cn.dairo.dfs.util.ObservableObject
import cn.dairo.ui.root.Toast
import java.io.ByteArrayOutputStream

class ProfileViewModel : ObservableObject() {

    companion object {

        /** 上传头像的最大边长,服务器最终会裁剪成256×256 */
        const val AVATAR_MAX_SIZE = 1024
    }

    /** 当前登录的用户信息 */
    var user by mutableStateOf(SettingStore.user)

    /** 昵称 */
    var nickname by mutableStateOf(SettingStore.user?.nickname ?: "")

    /** 是否有头像 */
    val hasAvatar: Boolean
        get() = !this.user?.avatar.isNullOrEmpty()

    /** 保存昵称点击事件 */
    fun onSaveNicknameClick() {
        this.nickname = this.nickname.trim()
        MineApi.nickname(this.nickname).send {
            Toast.show("保存成功")
            this.loadUser()
        }
    }

    /** 选择头像之后上传,选择器已经把图片缩小到最大边长 */
    fun onAvatarPick(image: Bitmap) {
        val output = ByteArrayOutputStream()
        if (!image.compress(Bitmap.CompressFormat.JPEG, 90, output)) {
            Toast.show("读取图片失败")
            return
        }
        MineApi.uploadAvatar(output.toByteArray()).send {
            this.loadUser()
        }
    }

    /** 删除头像点击事件 */
    fun onDeleteAvatarClick() {
        MineApi.deleteAvatar().send {
            this.loadUser()
        }
    }

    /** 刷新用户信息 */
    private fun loadUser() {
        MineApi.info().send {
            SettingStore.user = it
            this.user = it
        }
    }
}
