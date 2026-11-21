package cn.dairo.dfs

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import cn.dairo.dfs.components.DfsImageLoader
import cn.dairo.dfs.page.systemalbum.view.clearSystemAlbumThumbCache
import cn.dairo.dfs.util.FileBackgroundTransfer
import cn.dairo.dfs.util.FileSdkUtil

class DairoDFSApp : Application() {

    override fun onCreate() {
        super.onCreate()

        //提前创建SDK实例,App重启后未完成的下载和上传会自动继续
        runCatching { FileSdkUtil.get() }

        //让上传和下载在App进入后台后继续
        FileBackgroundTransfer.start()

        //App前后台状态
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {

            //回到前台
            override fun onStart(owner: LifecycleOwner) {
                FileSdkUtil.checkServer()
                FileBackgroundTransfer.onForeground()
            }

            //进入后台
            override fun onStop(owner: LifecycleOwner) {
                FileSdkUtil.flush()
            }
        })
    }

    /** 界面全部隐藏(进入后台)或者内存紧张时,释放图片的内存缓存,减少在后台占用的内存 */
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= TRIM_MEMORY_UI_HIDDEN) {
            DfsImageLoader.clearMemory()
            clearSystemAlbumThumbCache()
        }
    }
}
