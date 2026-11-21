package cn.dairo.dfs.page.albumviewer.viewmodel

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import cn.dairo.dfs.api.model.FileModel
import cn.dairo.dfs.extension.dliveVideoPreviewSource
import cn.dairo.dfs.extension.dliveVideoSource
import cn.dairo.dfs.extension.downloadSource
import cn.dairo.dfs.util.DliveUtil
import cn.dairo.dfs.util.FileSdkUtil
import cn.dairo.dfs.util.ObservableObject
import cn.dairo.dfs.util.fileSdkMessage
import cn.dairo.file.sdk.FetchTask
import cn.dairo.ui.DairoUI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 实况照片的视频,打开照片时播放3秒
 */
class AlbumViewerDliveViewModel : ObservableObject() {

    companion object {

        /** 实况照片是否播放声音,App每次启动时默认关闭,打开之后在App运行期间一直保持 */
        var isDliveSoundOn = false
    }

    /** 下载进度信息 */
    var progress by mutableStateOf("")

    /** 是否显示 */
    var isShow by mutableStateOf(false)

    private var fm: FileModel? = null

    /** 实况照片视频播放器 */
    var dlivePlayer by mutableStateOf<ExoPlayer?>(null)
        private set

    /** 是否播放声音 */
    var isSoundOn by mutableStateOf(isDliveSoundOn)

    /** 当前下载任务 */
    private var fetchTask: FetchTask? = null

    /** 拆分视频、播放结束的任务 */
    private var job: Job? = null

    fun setFm(fm: FileModel) {
        this.isShow = true
        this.fm = fm
        this.load()
    }

    private fun load() {
        val fm = this.fm ?: return
        val videoPath = FileSdkUtil.cachedPath(fm.dliveVideoSource())
        if (videoPath != null) {//如果视频文件已经生成
            this.play(videoPath)
            return
        }
        val path = FileSdkUtil.cachedPath(fm.downloadSource)
        if (path != null) {//如果文件已经下载,拆出视频存入缓存
            this.job = this.scope.launch {
                val savedPath = withContext(Dispatchers.IO) {
                    DliveUtil.getInfo(path)?.let { FileSdkUtil.putData(fm.dliveVideoSource(it.videoExt), it.videoData) }
                }
                savedPath?.let { play(it) }
            }
            return
        }

        //优先加载下载好的预览视频
        val previewPath = FileSdkUtil.cachedPath(fm.dliveVideoPreviewSource)
        if (previewPath != null) {
            this.play(previewPath)
            return
        }
        this.fetchTask = FileSdkUtil.fetch(fm.dliveVideoPreviewSource, progress = {
            this.progress = "$it%"
        }) { fetchPath, error ->
            this.fetchTask = null
            this.progress = error?.fileSdkMessage ?: ""
            if (fetchPath != null) {
                this.play(fetchPath)
            }
        }
    }

    /** 播放视频,3秒之后结束 */
    private fun play(path: String) {

        //关闭声音时不打断其他App正在播放的音乐
        val player = ExoPlayer.Builder(DairoUI.context)
            .setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(), this.isSoundOn)
            .build()
        player.volume = if (this.isSoundOn) 1f else 0f
        player.setMediaItem(MediaItem.fromUri(Uri.fromFile(File(path))))
        player.playWhenReady = true
        player.prepare()
        this.dlivePlayer = player
        this.job = this.scope.launch {
            delay(3000)
            if (dlivePlayer === player) {//已经切换到其他照片或者重新播放时不要结束
                clear()
            }
        }
    }

    /** 声音开关点击事件,打开时重新播放一次 */
    fun onSoundClick() {
        this.isSoundOn = !this.isSoundOn
        isDliveSoundOn = this.isSoundOn
        val fm = this.fm
        if (this.isSoundOn && fm != null) {
            this.clear()
            this.setFm(fm)
        } else {
            this.dlivePlayer?.volume = 0f
        }
    }

    fun clear() {
        this.job?.cancel()
        this.job = null
        this.fetchTask?.cancel()
        this.fetchTask = null
        this.dlivePlayer?.release()
        this.dlivePlayer = null
        this.isShow = false
    }
}
