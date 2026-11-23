package cn.dairo.dfs.util

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import cn.dairo.dfs.MainActivity
import cn.dairo.dfs.R
import cn.dairo.file.sdk.CacheMode
import cn.dairo.file.sdk.CacheQuery
import cn.dairo.file.sdk.DownloadState
import cn.dairo.file.sdk.Event
import cn.dairo.file.sdk.UploadQuery
import cn.dairo.file.sdk.UploadState
import cn.dairo.ui.DairoUI
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 让上传和永久下载在App进入后台后继续,对应iOS的FileBackgroundTransfer
 * 有传输时启动前台服务,在通知栏显示进度,传输全部结束之后停止服务
 */
object FileBackgroundTransfer {

    private const val TAG = "FileBackgroundTransfer"

    private val scope = MainScope()

    /** 是否已经申请过通知权限,每次启动只申请一次 */
    private var isPermissionRequested = false

    /**
     * 开始监听传输,只调用一次
     */
    fun start() {
        this.scope.launch {
            FileSdkUtil.events.collect {
                if (isTransferStarted(it)) {
                    startService()
                }
            }
        }
    }

    /** App回到前台时调用,后台时启动服务可能被系统拒绝,这里再检查一次 */
    fun onForeground() {
        if (this.activeTransfers().isNotEmpty()) {
            this.startService()
        }
    }

    /** 事件是否表示有传输开始了,缓存图片和视频不算在内 */
    private fun isTransferStarted(event: Event): Boolean {
        return when (event) {
            is Event.StateChanged -> event.mode == CacheMode.SAVE && (event.state == DownloadState.WAITING || event.state == DownloadState.DOWNLOADING)
            is Event.UploadStateChanged -> event.state == UploadState.WAITING || event.state == UploadState.UPLOADING
            else -> false
        }
    }

    /**
     * 查询正在进行的传输
     * @return 文件ID => (文件大小, 已传输大小)
     */
    fun activeTransfers(): Map<String, Pair<Long, Long>> {
        val sdk = runCatching { FileSdkUtil.get() }.getOrNull() ?: return emptyMap()
        val result = HashMap<String, Pair<Long, Long>>()
        for (state in listOf(DownloadState.WAITING, DownloadState.DOWNLOADING)) {
            val query = CacheQuery(group = null, mode = CacheMode.SAVE, state = state, offset = 0UL, limit = Int.MAX_VALUE.toULong())
            for (it in runCatching { sdk.list(query) }.getOrDefault(emptyList())) {
                result["download-${it.id}"] = Pair((it.size ?: it.downloaded).toLong(), it.downloaded.toLong())
            }
        }
        val account = FileSdkUtil.dfsAccount
        if (account != null) {
            for (state in listOf(UploadState.WAITING, UploadState.UPLOADING)) {
                val query = UploadQuery(account = account, state = state, offset = 0UL, limit = Int.MAX_VALUE.toULong())
                for (it in runCatching { sdk.listUploads(query) }.getOrDefault(emptyList())) {
                    result["upload-${it.id}"] = Pair((it.size ?: it.uploaded).toLong(), it.uploaded.toLong())
                }
            }
        }
        return result
    }

    /** 启动前台服务,Android 13开始要先申请通知权限才能显示进度 */
    private fun startService() {
        if (FileTransferService.isRunning) {
            return
        }
        val context = DairoUI.context
        //主界面在后台时(比如从其他App分享文件)不申请,否则会把主界面带到前台,下次在App里传输时再申请
        val activity = AppActivity.current as? ComponentActivity
        val isForeground = activity?.lifecycle?.currentState?.isAtLeast(Lifecycle.State.STARTED) == true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !this.isPermissionRequested && isForeground &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            this.isPermissionRequested = true
            ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 0)
        }
        try {
            ContextCompat.startForegroundService(context, Intent(context, FileTransferService::class.java))
        } catch (e: Exception) {

            //App在后台时不允许启动前台服务,回到前台时再启动
            Log.w(TAG, "启动传输服务失败", e)
        }
    }
}

/**
 * 传输文件的前台服务,只用来在后台保持运行和显示进度,传输由SDK完成
 */
class FileTransferService : Service() {

    companion object {

        /** 通知渠道ID */
        private const val CHANNEL_ID = "file_transfer"

        /** 通知ID */
        private const val NOTIFICATION_ID = 1

        /** 检查传输进度的间隔(毫秒) */
        private const val REFRESH_INTERVAL = 1000L

        /** 服务是否正在运行 */
        var isRunning = false
            private set
    }

    private val scope = MainScope()

    private var job: Job? = null

    /** 本次运行期间每个文件的大小和已传输大小,传输完成的文件也保留,保证总进度不会倒退 */
    private val transfers = HashMap<String, Pair<Long, Long>>()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, "文件传输", NotificationManager.IMPORTANCE_LOW))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC else 0
        ServiceCompat.startForeground(this, NOTIFICATION_ID, this.makeNotification(0, 0, 0), type)
        if (this.job == null) {
            this.job = this.scope.launch {
                while (isActive) {
                    if (!refresh()) {
                        break
                    }
                    delay(REFRESH_INTERVAL)
                }
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    /**
     * 更新通知栏的进度
     * @return 还有没有正在进行的传输
     */
    private fun refresh(): Boolean {
        val active = FileBackgroundTransfer.activeTransfers()
        this.transfers.putAll(active)

        //刚离开列表的文件,按传输完成计算,暂停和失败的文件也不再等待
        for ((id, transfer) in this.transfers.entries.toList()) {
            if (!active.containsKey(id)) {
                this.transfers[id] = Pair(transfer.first, transfer.first)
            }
        }
        if (active.isEmpty()) {
            FileSdkUtil.flush()
            return false
        }
        val total = this.transfers.values.sumOf { it.first }
        val done = this.transfers.values.sumOf { it.second }
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, this.makeNotification(total, done, active.size))
        return true
    }

    /**
     * 通知栏显示的进度
     * @param count 还剩下的文件数
     */
    private fun makeNotification(total: Long, done: Long, count: Int): Notification {
        val intent = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val max = 1000
        val progress = if (total > 0) (done.toDouble() / total * max).toInt().coerceIn(0, max) else 0
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_transfer)
            .setContentTitle("正在传输文件")
            .setContentText(if (count > 0) "还剩${count}个文件" else "准备中")
            .setProgress(max, progress, total == 0L)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(intent)
            .build()
    }

    override fun onDestroy() {
        isRunning = false
        this.scope.cancel()
        super.onDestroy()
    }
}
