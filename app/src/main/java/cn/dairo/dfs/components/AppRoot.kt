package cn.dairo.dfs.components

import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cn.dairo.dfs.constants.FunctionMode
import cn.dairo.dfs.page.album.AlbumPage
import cn.dairo.dfs.page.file.FilePage
import cn.dairo.dfs.page.login.LoginPage
import cn.dairo.dfs.store.SettingStore
import cn.dairo.ui.root.RootView

/**
 * App的根页面,对应iOS的WindowGroup
 * relaunch替换整个页面(关闭其他所有页面)
 */
object AppRoot {

    /** 启动时的页面,根据登录状态和功能模式决定 */
    private val launchPage: @Composable () -> Unit = {
        if (!SettingStore.isLogin) {
            LoginPage()
        } else if (SettingStore.functionMode == FunctionMode.ALBUM) {
            AlbumPage()
        } else {
            FilePage()
        }
    }

    /** 当前的根页面 */
    internal var page by mutableStateOf(launchPage)

    /** 每次relaunch都重新创建页面,之前页面的状态全部丢弃 */
    internal var pageKey by mutableIntStateOf(0)

    private val handler = Handler(Looper.getMainLooper())

    internal fun relaunch(page: @Composable () -> Unit) {
        val block = {
            this.page = page
            this.pageKey += 1
        }
        if (Looper.myLooper() == Looper.getMainLooper()) block() else handler.post(block)
    }

    /**
     * 回到启动时的页面
     * 按返回键退出后进程可能还在,再次打开时不能沿用上次的页面(比如从相册页临时退出到的文件页)
     */
    internal fun reset() {
        this.relaunch(this.launchPage)
    }
}

/**
 * 关闭其他所有页面,跳转到这个页面,对应iOS的View.relaunch()
 */
fun relaunch(page: @Composable () -> Unit) {
    AppRoot.relaunch(page)
}

/**
 * App的根视图
 */
@Composable
fun AppRootView() {
    RootView {
        key(AppRoot.pageKey) {
            AppRoot.page()
        }
    }
}
