package cn.dairo.dfs

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import cn.dairo.dfs.components.AppRoot
import cn.dairo.dfs.components.AppRootView
import cn.dairo.dfs.util.AppActivity

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AppActivity.attach(this)

        //全新打开时回到启动页面;旋转屏幕等重建时保留当前页面
        if (savedInstanceState == null) {
            AppRoot.reset()
        }

        //首页按返回键时关闭界面,下次打开时重新按功能模式选择首页
        //Android 12开始系统默认只把App退到后台,不关闭界面,再打开时还是之前的页面
        //比页面里的BackHandler先注册,优先级最低,只有页面都不处理返回键时才会执行
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finishAndRemoveTask()
            }
        })
        setContent {
            AppRootView()
        }
    }
}
