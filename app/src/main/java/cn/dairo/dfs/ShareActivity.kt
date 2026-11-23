package cn.dairo.dfs

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.IntentCompat
import cn.dairo.dfs.page.sharefolder.ShareFolderPage
import cn.dairo.dfs.page.sharefolder.ShareFolderViewModel
import cn.dairo.dfs.util.rememberStateObject
import cn.dairo.ui.root.RootView

/**
 * 接收其他App分享的文件(图片、视频和其他文件),选择文件夹之后上传,对应iOS的分享扩展ShareViewController
 * 和iOS不同,这里是App自己的界面,直接加入SDK的上传任务,由前台服务在后台上传
 */
class ShareActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        //纯文字和链接没有文件,不处理
        val uris = this.sharedUris(this.intent)
        if (uris.isEmpty()) {
            android.widget.Toast.makeText(this, "没有可以上传的文件", android.widget.Toast.LENGTH_SHORT).show()
            this.finish()
            return
        }
        setContent {
            RootView {
                val vm = rememberStateObject {
                    ShareFolderViewModel(uris) { count ->

                        //分享页面马上关闭,用系统的提示,回到原来的App之后也能看到
                        android.widget.Toast.makeText(applicationContext, "已添加${count}个文件到上传列表", android.widget.Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
                ShareFolderPage(vm, onCancel = { finish() })
            }
        }
    }

    /** 分享的文件地址 */
    private fun sharedUris(intent: Intent): List<Uri> {
        val uris = when (intent.action) {
            Intent.ACTION_SEND -> listOfNotNull(IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java))
            Intent.ACTION_SEND_MULTIPLE -> IntentCompat.getParcelableArrayListExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)?.filterNotNull() ?: emptyList()
            else -> emptyList()
        }

        //只接收content://,file://可能被其他App用来读取DairoDFS自己的私有文件
        return uris.filter { it.scheme == "content" }.distinct()
    }
}
