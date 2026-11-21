package cn.dairo.dfs.page.audioplayer

import android.content.Intent
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * 音频的后台播放、通知栏和锁屏的播放控制,对应iOS的后台音频和MPRemoteCommandCenter
 * 播放器由播放页面创建,页面连接服务时把播放器交给服务,页面关闭时停止服务
 */
class AudioPlaybackService : MediaSessionService() {

    companion object {

        /** 要播放的播放器,连接服务之前设置 */
        var player: ExoPlayer? = null

        /** 当前运行的服务 */
        private var current: AudioPlaybackService? = null

        /** 停止播放之后关闭服务和通知 */
        fun stop() {
            this.player = null
            this.current?.let {
                it.mediaSession?.release()
                it.mediaSession = null
                it.stopSelf()
            }
        }
    }

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        current = this
        player?.let {
            this.mediaSession = MediaSession.Builder(this, it).build()
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return this.mediaSession
    }

    /** App从最近任务里划掉时停止播放 */
    override fun onTaskRemoved(rootIntent: Intent?) {
        this.mediaSession?.player?.pause()
        stopSelf()
    }

    override fun onDestroy() {
        this.mediaSession?.release()
        this.mediaSession = null
        if (current === this) {
            current = null
        }
        super.onDestroy()
    }
}
