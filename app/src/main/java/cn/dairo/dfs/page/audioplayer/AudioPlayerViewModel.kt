package cn.dairo.dfs.page.audioplayer

import android.content.ComponentName
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import cn.dairo.dfs.api.model.FileModel
import cn.dairo.dfs.extension.downloadSource
import cn.dairo.dfs.util.FileSdkUtil
import cn.dairo.dfs.util.ObservableObject
import cn.dairo.ui.DairoUI
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

class AudioPlayerViewModel(fm: FileModel) : ObservableObject() {

    /** 文件名 */
    val name = fm.name

    /** 是否正在播放 */
    var isPlaying by mutableStateOf(false)

    /** 当前播放位置(秒) */
    var currentTime by mutableDoubleStateOf(0.0)

    /** 音频总时长(秒),还没加载出来时为0 */
    var duration by mutableDoubleStateOf(0.0)

    /** 加载失败时的提示 */
    var errorMsg by mutableStateOf<String?>(null)

    /** 是否正在拖动进度条,拖动时不更新播放位置 */
    var isDragging = false

    private var player: ExoPlayer? = null

    /** 连接后台播放服务,连接之后才有通知栏和锁屏的播放控制 */
    private var controllerFuture: ListenableFuture<MediaController>? = null

    init {
        val source = fm.downloadSource

        //已经下载的直接播放文件,否则通过SDK边下边播
        val uri = FileSdkUtil.cachedPath(source)?.let { Uri.fromFile(File(it)) } ?: FileSdkUtil.open(source)?.let { Uri.parse(it) }
        if (uri == null) {
            this.errorMsg = "音频加载失败"
        } else {
            this.play(uri)
        }
    }

    private fun play(uri: Uri) {
        val context = DairoUI.context

        //播放时暂停其他App的声音,拔出耳机时暂停
        val player = ExoPlayer.Builder(context)
            .setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(), true)
            .setHandleAudioBecomingNoisy(true)
            .build()
        player.addListener(object : Player.Listener {

            //来电等情况系统会暂停播放,这里以播放器的实际状态为准
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                this@AudioPlayerViewModel.isPlaying = isPlaying || player.playWhenReady
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                this@AudioPlayerViewModel.isPlaying = playWhenReady
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (player.duration != C.TIME_UNSET) {
                    duration = player.duration / 1000.0
                }

                //播放结束后回到开头
                if (playbackState == Player.STATE_ENDED) {
                    player.pause()
                    seek(0.0)
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                errorMsg = "音频加载失败,请检查网络或访问权限"
            }
        })
        player.setMediaItem(MediaItem.Builder().setUri(uri).setMediaMetadata(MediaMetadata.Builder().setTitle(this.name).build()).build())
        player.playWhenReady = true
        player.prepare()
        this.player = player
        AudioPlaybackService.player = player
        this.controllerFuture = MediaController.Builder(context, SessionToken(context, ComponentName(context, AudioPlaybackService::class.java))).buildAsync()

        //更新播放位置
        this.scope.launch {
            while (isActive) {
                if (!isDragging) {
                    currentTime = player.currentPosition / 1000.0
                }
                delay(500)
            }
        }
    }

    /** 播放/暂停按钮点击事件 */
    fun onPlayClick() {
        val player = this.player ?: return
        if (player.playWhenReady) {
            player.pause()
        } else {
            player.play()
        }
    }

    /** 跳转到指定位置(秒) */
    fun seek(seconds: Double) {
        this.currentTime = seconds
        this.player?.seekTo((seconds * 1000).toLong())
    }

    /** 关闭播放页面时停止播放 */
    override fun onCleared() {
        this.controllerFuture?.let { MediaController.releaseFuture(it) }
        this.controllerFuture = null
        AudioPlaybackService.stop()
        this.player?.release()
        this.player = null
        super.onCleared()
    }
}
