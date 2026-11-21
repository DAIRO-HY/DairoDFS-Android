package cn.dairo.dfs.page.albumviewer.viewmodel

import android.net.Uri
import android.view.Choreographer
import androidx.annotation.OptIn
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.compose.ui.unit.Velocity
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import cn.dairo.dfs.api.FilesApi
import cn.dairo.dfs.api.http.ApiHttpBase
import cn.dairo.dfs.api.model.FileModel
import cn.dairo.dfs.api.model.FilePropertyModel
import cn.dairo.dfs.components.DfsImageLoader
import cn.dairo.dfs.extension.downloadSource
import cn.dairo.dfs.extension.isDlive
import cn.dairo.dfs.extension.isVideo
import cn.dairo.dfs.extension.previewImageSource
import cn.dairo.dfs.extension.previewVideoSource
import cn.dairo.dfs.extension.propertySource
import cn.dairo.dfs.extension.screenImageSource
import cn.dairo.dfs.extension.thumbSource
import cn.dairo.dfs.page.album.AlbumPage
import cn.dairo.dfs.store.SettingStore
import cn.dairo.dfs.util.DliveUtil
import cn.dairo.dfs.util.FileSdkUtil
import cn.dairo.dfs.util.FileSource
import cn.dairo.dfs.util.MediaStoreUtil
import cn.dairo.dfs.util.NotificationCenter
import cn.dairo.dfs.util.ObservableObject
import cn.dairo.dfs.util.fileSdkMessage
import cn.dairo.file.sdk.DownloadState
import cn.dairo.file.sdk.Event
import cn.dairo.file.sdk.FetchTask
import cn.dairo.file.sdk.SdkException
import cn.dairo.ui.DairoUI
import cn.dairo.ui.root.Toast
import cn.dairo.ui.util.format
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.io.ByteArrayInputStream
import java.io.File
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * 查看页面的视图模型,单位都是像素
 */
@OptIn(UnstableApi::class)
class AlbumViewerViewModel(fileModels: List<FileModel>, index: Int) : ObservableObject() {

    companion object {

        /**
         * 页面切换动画时间(毫秒)
         * 该时间不宜过长,过长可能导致用户快速切换时显示的图片错乱
         * 该时间也不宜过短,过短会让页面闪烁一下,看起来不爽
         */
        const val ANIMATION_TIME = 200

        /** 轻扫切换页面的速度(dp/秒),松手时超过这个速度就算轻扫 */
        const val FLICK_VELOCITY = 500f

        /** 惯性滑动每毫秒的速度衰减,和iOS系统的默认值一样 */
        const val INERTIA_DECAY = 0.998

        /** 松手时速度低于这个值(dp/秒)时没有惯性滑动 */
        const val INERTIA_MIN_VELOCITY = 100f

        /** 惯性滑动速度低于这个值(dp/秒)时停止 */
        const val INERTIA_STOP_VELOCITY = 20f

        /** 拖动超出边界时的阻力 */
        const val RUBBER_BAND_RATE = 0.55f

        /** 惯性滑动超出边界时,弹回去的弹簧强度 */
        const val BOUNCE_STIFFNESS = 300f

        /** 惯性滑动超出边界时,弹回去的阻尼,这个值刚好不会来回晃动 */
        const val BOUNCE_DAMPING = 35f

        /** 双击屏幕时视频快进、快退的秒数 */
        const val VIDEO_SEEK_SECONDS = 10.0

        /** 按住屏幕时视频的播放倍速 */
        const val VIDEO_SPEED_UP_RATE = 2f

        /** 手机解码不了视频时的错误,和网络错误区分开 */
        private val DECODER_ERROR_CODES = setOf(
            PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
            PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED,
            PlaybackException.ERROR_CODE_DECODING_FAILED,
            PlaybackException.ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES,
            PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED,
        )

        /** 原图解码之后最长边的最大像素数,放大查看时也足够清晰 */
        private const val ORIGINAL_MAX_SIZE = 4096

        private val json = Json { ignoreUnknownKeys = true }
    }

    /** 当前视图区域的尺寸 */
    var screenWidth by mutableFloatStateOf(0f)
    var screenHeight by mutableFloatStateOf(0f)

    /** 屏幕密度,速度的阈值按dp计算 */
    private var density = 1f

    /** 文件列表 */
    var fileModels by mutableStateOf(fileModels)
        private set

    /** 获取文件属性的http请求 */
    private var propertyApiHttp: ApiHttpBase? = null

    /** 当前图片的下载任务 */
    private var fetchTask: FetchTask? = null

    /** 当前图片缩略图的下载任务,和图片同时下载 */
    private var thumbFetchTask: FetchTask? = null

    /** 当前文件属性 */
    var fileProperty by mutableStateOf<FilePropertyModel?>(null)
        private set

    /** 文件属性是否正在加载 */
    var propertyIsLoading by mutableStateOf(false)
        private set

    /** 属性面板是否显示 */
    var showInfo by mutableStateOf(false)

    /** 当前文件的一些标签（RAW、4K、60FPS......），显示在左上角 */
    var tags by mutableStateOf(emptyList<String>())

    /** 当前文件的时间,显示在顶部,优先显示拍摄时间,没有拍摄时间时显示上传时间 */
    var fileTime by mutableStateOf<String?>(null)

    /** 当前选中的序号 */
    var currentIndex by mutableIntStateOf(index)
        private set

    /** 页面的水平偏移量,当前页面显示在屏幕上时是-(currentIndex + 1) * screenWidth */
    var hStackOffset by mutableFloatStateOf(0f)

    /** 缩放比例 */
    var zoomAmount by mutableFloatStateOf(1f)

    /** 图片的偏移位置 */
    var currentOffsetPosition by mutableStateOf(Offset.Zero)

    /** 放大后拖动图片,松手之后的惯性滑动速度(像素/秒) */
    private var inertiaVelocity = Offset.Zero

    /** 惯性滑动的逐帧回调,不为null时表示正在惯性滑动 */
    private var inertiaCallback: Choreographer.FrameCallback? = null

    /** 是否正在惯性滑动 */
    val isInertiaRunning: Boolean
        get() = this.inertiaCallback != null

    /** 正在进行的回弹、缩放动画 */
    private var animationJob: Job? = null

    /** 当前图片 */
    var uiImage by mutableStateOf<ImageBitmap?>(null)

    /** 当前显示的图片的清晰度,更清晰的图片显示之后,后加载完成的模糊图片不再显示 0:没有 1:缩略图 2:屏幕大小 3:原图 */
    private var imageLevel = 0

    /** 图片加载失败的错误信息 */
    var errorMsg by mutableStateOf("")

    /** 图片是否因为网络问题加载失败,在右上角显示联网失败的图标,不显示错误信息 */
    var isNetworkFail by mutableStateOf(false)

    /** 大图、原图加载中的进度,比如:35%,还没有进度时是空字符串,没有加载时为null */
    private var loadingTip by mutableStateOf<String?>(null)

    /** 下载、保存的进度,格式和loadingTip一样,没有下载时为null */
    private var downloadTip by mutableStateOf<String?>(null)

    /** 加载提示里显示的进度,正在下载时优先显示下载进度 */
    val progressTip: String?
        get() = this.downloadTip ?: this.loadingTip

    /** 是否正在下载、保存 */
    val isDownloading: Boolean
        get() = this.downloadTip != null

    /** 图片宽高比 */
    private var uiImageWHRate = 0f

    /** 屏幕宽高比 */
    private var screenWHRate = 0f

    /** 没有缩放状态下的显示宽与高 */
    private var displayW1 = 0f
    private var displayH1 = 0f

    /** 每次切换页面加1,异步任务完成时用来判断是否还是同一个页面 */
    private var pageVersion = 0

    /** 文件Model */
    val fm: FileModel
        get() = this.fileModels[this.currentIndex]

    /** 这是否是一个视频 */
    var isVideo by mutableStateOf(false)
        private set

    /** 是否图片 */
    val isImage: Boolean
        get() = !this.isVideo

    /** 标记当前加载的图片或者视频是否已经下载 */
    var isShowDownloaded by mutableStateOf(false)

    /** 下载完成之后是否保存到系统相册 */
    private var isSaveToAlbum = false

    /** 是否显示操作按钮 */
    var showActionView by mutableStateOf(true)

    /** 记录进度条是否正在拖拽中 */
    var videoSliderDragging = false

    /** 视频播放器 */
    var videoPlayer by mutableStateOf<ExoPlayer?>(null)
        private set

    /** 视频总时间(毫秒) */
    var videoDuration by mutableDoubleStateOf(1.0)

    /** 视频当前播放时间(毫秒) */
    var videoCurrentTime by mutableDoubleStateOf(0.0)

    /** 视频是否正在播放 */
    var videoIsPlaying by mutableStateOf(false)

    /** 标记视频是否播放过 */
    var videoIsPlayed by mutableStateOf(false)

    /** 标记当前播放的视频是否原画 */
    private var videoIsOriginal = false

    /** 视频是否正在加载(等待文件属性或者视频信息),这时不能点击播放 */
    var videoIsLoading by mutableStateOf(false)

    /** 视频是否正在按住屏幕倍速播放 */
    var videoIsSpeedUp by mutableStateOf(false)

    /** 快进、快退提示的秒数,负数代表快退,null代表不显示 */
    var videoSeekTip by mutableStateOf<Double?>(null)

    /** 用来让快进、快退提示自动消失的任务 */
    private var videoSeekTipJob: Job? = null

    /** 刷新播放进度的任务 */
    private var videoTimerJob: Job? = null

    /** 当前视频质量 */
    var videoQualityLabel by mutableStateOf(SettingStore.videoQualityLabel)

    /** 是否需要重新播放视频，而不是接着上次播放 */
    private var isVideoReplay = true

    val dliveVm = AlbumViewerDliveViewModel()

    /**
     * 视图初始化时
     * @param density 屏幕密度
     */
    fun onAppear(screenWidth: Float, screenHeight: Float, density: Float) {
        this.density = density
        this.screenWidth = screenWidth
        this.screenHeight = screenHeight

        //屏幕宽高比
        this.screenWHRate = this.screenWidth / this.screenHeight

        //为了保证图片未加载时也能正常切换,默认将屏幕大小作为图片大小
        this.displayW1 = this.screenWidth
        this.displayH1 = this.screenHeight
        this.uiImageWHRate = this.screenWHRate
        this.changePage(this.currentIndex)
    }

    /** 视图尺寸变化时(屏幕旋转、分屏等),只更新布局,不重新加载文件,视频会继续播放 */
    fun onResize(screenWidth: Float, screenHeight: Float) {
        this.screenWidth = screenWidth
        this.screenHeight = screenHeight
        this.screenWHRate = this.screenWidth / this.screenHeight
        this.stopInertia()
        this.animationJob?.cancel()
        this.hStackOffset = this.pageOffset
        this.currentOffsetPosition = Offset.Zero
        this.zoomAmount = 1f
        val image = this.uiImage
        this.uiImageWHRate = if (image != null) image.width.toFloat() / image.height else this.screenWHRate
        this.computeDisplaySize()
    }

    /** 当前页面显示在屏幕上时的页面偏移量 */
    private val pageOffset: Float
        get() = -(this.currentIndex + 1) * this.screenWidth

    /**
     * 动画修改缩放、偏移和页面位置,对应iOS的withAnimation
     * 先执行block得到最终的值,再从原来的值逐渐变化过去
     */
    private fun withAnimation(block: () -> Unit) {
        this.animationJob?.cancel()
        val fromZoom = this.zoomAmount
        val fromOffset = this.currentOffsetPosition
        val fromPage = this.hStackOffset
        block()
        val toZoom = this.zoomAmount
        val toOffset = this.currentOffsetPosition
        val toPage = this.hStackOffset
        if (fromZoom == toZoom && fromOffset == toOffset && fromPage == toPage) {
            return
        }
        this.zoomAmount = fromZoom
        this.currentOffsetPosition = fromOffset
        this.hStackOffset = fromPage

        //动画需要逐帧的时钟,视图模型的协程里没有,使用界面的主线程调度器
        this.animationJob = this.scope.launch(AndroidUiDispatcher.Main) {
            Animatable(0f).animateTo(1f, tween(ANIMATION_TIME, easing = FastOutSlowInEasing)) {
                zoomAmount = fromZoom + (toZoom - fromZoom) * value
                currentOffsetPosition = fromOffset + (toOffset - fromOffset) * value
                hStackOffset = fromPage + (toPage - fromPage) * value
            }
        }
    }

    /**
     * 松手之后决定切换页面还是回弹
     * @param velocity 松手时手指的水平速度,向右为正
     */
    private fun fixPageViewPosition(velocity: Float = 0f) {

        //页面切换滑动距离
        val threshold = this.screenWidth / 4

        //本次拖拽的距离 = 当前页偏移位置 - 上页偏移位置
        val dragDistance = this.pageOffset - this.hStackOffset
        var currentIndex = this.currentIndex
        if (abs(velocity) > FLICK_VELOCITY * this.density) {//轻扫时看方向,距离再短也切换。往回扫时不切换
            if (dragDistance < 0 && velocity > 0) {
                currentIndex -= 1
            } else if (dragDistance > 0 && velocity < 0) {
                currentIndex += 1
            }
        } else if (dragDistance < -threshold) {//如果移动的距离达到了页面切换的距离
            currentIndex -= 1
        } else if (dragDistance > threshold) {
            currentIndex += 1
        }
        if (currentIndex !in this.fileModels.indices) {//如果切换页面超出了当前页面数量返回
            currentIndex = this.currentIndex
        }
        this.hStackOffset = -(currentIndex + 1) * this.screenWidth
        if (currentIndex != this.currentIndex) {//如果页面要切换

            //等待动画结束之后再切换页面,防止页面跳闪
            val version = this.pageVersion
            this.scope.launch {
                delay(ANIMATION_TIME + 50L)
                if (version == pageVersion) {
                    changePage(currentIndex)
                }
            }
        }
    }

    /** 页面切换 */
    private fun changePage(index: Int) {
        this.pageVersion += 1
        this.stopInertia()
        this.animationJob?.cancel()
        this.downloadTip = null
        this.isShowDownloaded = false
        this.isSaveToAlbum = false
        this.currentIndex = index
        this.uiImage = null
        this.imageLevel = 0
        this.hStackOffset = this.pageOffset
        this.currentOffsetPosition = Offset.Zero
        this.zoomAmount = 1f

        this.tags = emptyList()
        this.fileTime = null
        this.propertyApiHttp?.cancel()
        this.propertyIsLoading = false
        this.fileProperty = null
        this.cancelFetch()

        //停止视频
        this.videoIsPlaying = false
        this.videoIsPlayed = false
        this.stopVideoPlayer()
        this.videoIsOriginal = false
        this.videoIsLoading = false
        this.videoIsSpeedUp = false
        this.videoSeekTipJob?.cancel()
        this.videoSeekTip = null
        this.isVideoReplay = true

        this.isVideo = this.fm.isVideo

        //停止实况视频播放
        this.dliveVm.clear()
        this.errorMsg = ""
        this.isNetworkFail = false

        //图片和已下载的视频不依赖文件属性,不用等它加载完成,否则网络较差时要一直等待
        if (!this.isVideo) {
            this.loadPicture()
        } else if (FileSdkUtil.cachedPath(this.fm.downloadSource) != null) {
            this.onVideoPlayClick()
        }
        val fileProperty = FileSdkUtil.cachedPath(this.fm.propertySource)?.let { path ->
            runCatching { json.decodeFromString(FilePropertyModel.serializer(), File(path).readText()) }.getOrNull()
        }
        if (fileProperty != null) {//如果属性文件已经存在,解析失败时重新获取
            this.fileProperty = fileProperty
            this.initView()
        } else {
            this.loadProperty()
        }
    }

    /** 加载文件属性,图片加载失败时只是不显示时间和标签,不提示错误 */
    private fun loadProperty() {
        if (this.isVideo && this.videoPlayer == null) {
            this.videoIsLoading = true
        }
        this.propertyIsLoading = true
        val fm = this.fm
        this.propertyApiHttp = FilesApi.getProperty(listOf(fm.id)).hide().finish {
            this.propertyIsLoading = false
        }.error {
            this.onLoadPropertyFail(it)
        }.fail {
            this.onLoadPropertyFail(it.msg ?: "加载文件属性失败")
        }.send {
            this.onPropertyLoaded(fm, it)
        }
    }

    /** 文件属性加载完成 */
    private fun onPropertyLoaded(fm: FileModel, fileProperty: FilePropertyModel) {
        this.fileProperty = fileProperty
        if (fileProperty.state == 1) {//如果文件已经处理完成,则将文件属性信息缓存起来
            FileSdkUtil.putData(fm.propertySource, json.encodeToString(FilePropertyModel.serializer(), fileProperty).toByteArray())
        }
        this.initView()
    }

    /** 文件属性加载失败,视频没有属性不知道该播放哪个清晰度,恢复播放按钮,点击时重新加载 */
    private fun onLoadPropertyFail(msg: String) {
        if (this.isVideo && this.videoIsLoading) {
            this.videoIsLoading = false
            Toast.show(msg)
        }
    }

    /** 属性按钮点击事件 */
    fun onInfoClick() {
        if (!this.propertyIsLoading) {//缓存的属性可能已经过期(服务端补充了字段、其他设备改了拍摄时间),这里重新加载,失败时继续显示缓存
            this.loadProperty()
        }
        this.showInfo = true
    }

    /**
     * 修改拍摄时间
     * @param date 拍摄时间(毫秒),0代表恢复成照片本身的拍摄时间
     * @param finish 请求结束,失败时返回错误信息
     */
    fun updateCaptureTime(date: Long, finish: (error: String?) -> Unit) {

        //避免正在进行的加载覆盖修改结果
        this.propertyApiHttp?.cancel()
        this.propertyIsLoading = false
        val fm = this.fm
        FilesApi.updateCaptureTime(fm.id, date).hide().error {
            finish(it)
        }.fail {
            finish(it.msg ?: "修改拍摄时间失败")
        }.send {
            this.onPropertyLoaded(fm, it)

            //拍摄时间决定相册排序,通知相册列表页面更新
            NotificationCenter.post(AlbumPage.ALBUM_PAGE_RELOAD_DATA)
            finish(null)
        }
    }

    /** 初始化页面 */
    private fun initView() {
        if (this.isVideo && this.videoPlayer == null) {//视频时,已下载的视频已经开始播放
            this.onVideoPlayClick()
        }
        this.showTag()
    }

    /** 显示标签 */
    private fun showTag() {
        val tags = ArrayList<String>()
        if (FileSdkUtil.cachedPath(this.fm.downloadSource) != null) {
            tags.add("已下载")
        }
        var fileDate = this.fileProperty?.uploadDate ?: 0
        val property = this.fileProperty?.property?.let { runCatching { json.parseToJsonElement(it).jsonObject }.getOrNull() }
        if (property != null) {
            val date = property.long("date")
            if (date != null && date > 0) {//拍摄时间(毫秒)
                fileDate = date
            }
            val width = property.long("width")
            val height = property.long("height")
            if (width != null && height != null) {
                if (width * height == 3840L * 2160) {
                    tags.add("4K")
                } else if (width * height == 1920L * 1080) {
                    tags.add("HD")
                }
            }
            val fps = property["fps"]?.jsonPrimitive?.doubleOrNull?.toInt()
            if (fps != null && fps in 59..60) {
                tags.add("60FPS")
            }
        }
        if (fileDate > 0) {
            this.fileTime = Date(fileDate).format()
        }
        val lowerName = this.fm.name.lowercase()
        if (lowerName.endsWith(".cr3")) {
            tags.add("RAW")
        } else if (lowerName.endsWith(".dlive")) {
            tags.add("实况")
        }
        this.tags = tags
    }

    /** 读取JSON里的整数 */
    private fun JsonObject.long(key: String): Long? {
        return runCatching { this[key]?.jsonPrimitive?.longOrNull }.getOrNull()
    }

    /** 加载图片 */
    private fun loadPicture() {
        if (this.fm.isDlive) {//如果这是实况照片
            this.dliveVm.setFm(this.fm)
        }
        if (this.showCachedOriginal {
                this.loadThumb()
                this.loadScreenImage()
            }) {
            return
        }

        //先显示缩略图,避免网络较差时黑屏,再加载清晰的图片
        this.loadThumb()
        this.loadScreenImage()
    }

    /**
     * 显示已下载或已缓存的原图,解码在后台线程
     * @param onFail 有缓存但是无法显示时执行
     * @return 没有可以显示的图片时返回false
     */
    private fun showCachedOriginal(onFail: () -> Unit = {}): Boolean {
        val fm = this.fm
        val downloadPath = FileSdkUtil.cachedPath(fm.downloadSource)

        //RAW等格式没有下载原文件时,或者原文件无法显示时,看大图预览有没有缓存
        val previewPath = FileSdkUtil.cachedPath(fm.previewImageSource)
        if (downloadPath == null && previewPath == null) {
            return false
        }
        val version = this.pageVersion
        this.scope.launch {
            val image = withContext(Dispatchers.Default) {
                var data = downloadPath?.let { path ->
                    if (fm.isDlive) DliveUtil.getInfo(path)?.photoData else runCatching { File(path).readBytes() }.getOrNull()
                }
                var bitmap = data?.let { DfsImageLoader.decodeBitmap(it, ORIGINAL_MAX_SIZE) }
                if (bitmap == null && previewPath != null) {
                    data = runCatching { File(previewPath).readBytes() }.getOrNull()
                    bitmap = data?.let { DfsImageLoader.decodeBitmap(it, ORIGINAL_MAX_SIZE) }
                }
                bitmap?.asImageBitmap()
            }
            if (version != pageVersion) {
                return@launch
            }
            if (image == null) {
                onFail()
                return@launch
            }
            isShowDownloaded = true
            initImage(image, 3)
        }
        return true
    }

    /**
     * 在后台线程解码图片文件之后显示
     * @param level 图片的清晰度
     */
    private fun showImageFile(path: String, level: Int) {
        val version = this.pageVersion
        this.scope.launch {
            val image = withContext(Dispatchers.Default) {
                runCatching { File(path).readBytes() }.getOrNull()?.let { DfsImageLoader.decodeBitmap(it, ORIGINAL_MAX_SIZE) }?.asImageBitmap()
            }
            if (version == pageVersion && image != null) {
                initImage(image, level)
            }
        }
    }

    /** 加载缩略图,更清晰的图片已经显示时不再显示 */
    private fun loadThumb() {
        val path = FileSdkUtil.cachedPath(this.fm.thumbSource)
        if (path != null) {
            this.showImageFile(path, 1)
        } else if (this.fm.hasThumb) {
            this.thumbFetchTask = FileSdkUtil.fetch(this.fm.thumbSource, progress = {}) { thumbPath, _ ->
                this.thumbFetchTask = null
                if (thumbPath != null) {
                    this.showImageFile(thumbPath, 1)
                }
            }
        }
    }

    /** 加载适合当前屏幕的图片,服务器无法生成时直接加载原图 */
    private fun loadScreenImage() {
        val source = this.fm.screenImageSource
        val path = FileSdkUtil.cachedPath(source)
        if (path != null) {
            this.showImageFile(path, 2)
            return
        }
        this.fetch(source, notFound = this::loadOriginal) {
            this.showImageFile(it, 2)
        }
    }

    /** 加载原图 */
    private fun loadOriginal() {
        this.fetch(this.fm.previewImageSource) {
            this.showCachedOriginal()
        }
    }

    /**
     * 下载图片
     * @param notFound 文件不存在时执行,没有指定时显示错误信息
     * @param success 下载成功时执行
     */
    private fun fetch(source: FileSource, notFound: (() -> Unit)? = null, success: (path: String) -> Unit) {
        this.fetchTask?.cancel()
        this.loadingTip = ""
        this.fetchTask = FileSdkUtil.fetch(source, progress = {
            this.loadingTip = "$it%"
        }) { path, error ->
            this.fetchTask = null
            this.errorMsg = ""
            this.isNetworkFail = false
            this.loadingTip = null
            if (path != null) {
                success(path)
            } else if (notFound != null && error is SdkException.NotFound) {
                notFound()
            } else if (error is SdkException.Network) {//网络错误信息里有请求地址,不适合显示给用户
                this.isNetworkFail = true
            } else {
                this.errorMsg = error?.fileSdkMessage ?: ""
            }
        }
    }

    /** 取消下载图片 */
    fun cancelFetch() {
        this.fetchTask?.cancel()
        this.fetchTask = null
        this.loadingTip = null
        this.thumbFetchTask?.cancel()
        this.thumbFetchTask = null
    }

    /**
     * 双击事件
     * @param center 双击的位置,放大时以这个位置为中心
     */
    fun doubleClick(center: Offset) {
        this.withAnimation {
            if (this.zoomAmount > 1) {
                this.zoomAmount = 1f
            } else {

                //适合屏幕宽度的倍率
                val screenWidthZoom = this.screenWidth / this.displayW1

                //适合屏幕高度的倍率
                val screenHeightZoom = this.screenHeight / this.displayH1
                var zoom = max(screenWidthZoom, screenHeightZoom)
                if (screenWidthZoom == screenHeightZoom && screenWidthZoom == 1f) {//当前照片本身就是屏幕大小的比例
                    zoom = 2f
                }
                this.onImagePinch(zoom / this.zoomAmount, center, Offset.Zero)
            }
            this.fixCropImage()
        }
    }

    /**
     * 初始化图片显示
     * @param level 图片的清晰度,比当前显示的模糊时不显示
     */
    private fun initImage(image: ImageBitmap, level: Int) {
        if (level < this.imageLevel) {
            return
        }
        this.imageLevel = level
        this.errorMsg = ""
        this.isNetworkFail = false
        this.uiImage = image

        //图片宽高比
        this.uiImageWHRate = image.width.toFloat() / image.height
        this.computeDisplaySize()
    }

    /** 计算没有放大缩小的状态下,图片显示的宽与高 */
    private fun computeDisplaySize() {
        if (this.uiImageWHRate > this.screenWHRate) {//图片的宽高比大于屏幕的宽高比,则没有放大缩小的状态下,显示的宽度为屏幕宽度
            this.displayW1 = this.screenWidth
            this.displayH1 = this.displayW1 / this.uiImageWHRate
        } else {//图片的宽高比小于屏幕的宽高比,则没有放大缩小的状态下,显示的高度为屏幕的高度
            this.displayH1 = this.screenHeight
            this.displayW1 = this.displayH1 * this.uiImageWHRate
        }
    }

    /** 启动视频进度计时器,操作按钮显示时才需要 */
    fun startVideoTimer() {
        this.videoTimerJob?.cancel()
        this.videoTimerJob = this.scope.launch {
            while (isActive) {
                updateVideoTime()
                delay(500)
            }
        }
    }

    /** 停止视频进度计时器 */
    fun stopVideoTimer() {
        this.videoTimerJob?.cancel()
        this.videoTimerJob = null
    }

    /** 更新视频的播放时间和总时间 */
    private fun updateVideoTime() {
        if (this.videoSliderDragging) {//进度条正在拖拽中
            return
        }
        val player = this.videoPlayer ?: return
        if (player.duration == C.TIME_UNSET) {
            return
        }
        this.videoDuration = player.duration.toDouble()
        this.videoCurrentTime = player.currentPosition.toDouble()
    }

    /** 视频播放结束,回到开头并暂停 */
    private fun onVideoPlayEnd() {
        val player = this.videoPlayer ?: return
        this.stopVideoTimer()
        player.pause()
        player.seekTo(0)
        this.videoCurrentTime = 0.0
        this.videoIsPlaying = false
    }

    /** 暂停视频播放 */
    fun onVideoPauseClick() {
        this.videoPlayer?.pause()
        this.stopVideoTimer()
        this.videoIsPlaying = false
    }

    /** 视频播放按钮点击事件 */
    fun onVideoPlayClick() {
        if (this.fileProperty == null && FileSdkUtil.cachedPath(this.fm.downloadSource) == null) {//文件属性之前加载失败,重新加载,成功后自动播放
            this.loadProperty()
            return
        }
        if (this.isVideoReplay) {//强制重新播放
            this.videoPlay()
            return
        }
        val videoPlayer = this.videoPlayer
        if (videoPlayer != null) {//如果播放器仅仅是暂停，直接播放即可
            videoPlayer.play()

            //开启播放进度计时器
            this.startVideoTimer()
            this.videoIsPlaying = true
            return
        }
        this.videoPlay()
    }

    /** 停止播放视频,释放播放器 */
    fun stopVideoPlayer() {
        this.stopVideoTimer()
        val videoPlayer = this.videoPlayer ?: return
        videoPlayer.release()
        this.videoPlayer = null
    }

    /**
     * 视频快进、快退
     * @param seconds 跳转的秒数,负数代表快退
     */
    fun onVideoSeek(seconds: Double) {
        val player = this.videoPlayer ?: return
        if (player.duration == C.TIME_UNSET) {//视频还没有加载好
            return
        }
        val target = min(max(player.currentPosition + (seconds * 1000).toLong(), 0), player.duration)
        this.videoDuration = player.duration.toDouble()
        this.videoCurrentTime = target.toDouble()
        this.showVideoSeekTip(seconds)

        //播放到结尾时播放器会停止,跳回去之后要继续播放
        player.seekTo(target)
        if (this.videoIsPlaying) {
            player.play()
        }
    }

    /** 显示快进、快退提示,一段时间后自动消失 */
    private fun showVideoSeekTip(seconds: Double) {
        this.videoSeekTip = seconds
        this.videoSeekTipJob?.cancel()
        this.videoSeekTipJob = this.scope.launch {
            delay(800)
            videoSeekTip = null
        }
    }

    /** 按住屏幕倍速播放,松手恢复 */
    fun onVideoSpeedUpChange(isSpeedUp: Boolean) {
        this.videoIsSpeedUp = isSpeedUp
        this.videoPlayer?.setPlaybackSpeed(if (isSpeedUp) VIDEO_SPEED_UP_RATE else 1f)
    }

    /** 选择视频清晰度播放点击事件 */
    fun onVideoSelectResolutionClick(quality: Int) {
        SettingStore.videoQuality = quality
        this.videoQualityLabel = SettingStore.videoQualityLabel
        this.videoPlay()
    }

    /**
     * 播放点击事件
     * @param isForceSmooth 不管清晰度设置,直接在线播放流畅视频,原画超出手机解码能力时使用
     */
    private fun videoPlay(isForceSmooth: Boolean = false) {
        this.videoIsPlayed = true//标记视频播放过
        this.isVideoReplay = false

        //记录当前播放时间，方便切换清晰度之后继续播放
        val currentTime = this.videoPlayer?.currentPosition ?: 0

        //切换清晰度时先停掉之前的播放器,否则之前的视频会继续播放,出现多个声音
        this.stopVideoPlayer()
        val path = FileSdkUtil.cachedPath(this.fm.downloadSource)
        if (path != null && !isForceSmooth) {//如果该视频文件已经被下载,直接播放已经下载的文件
            this.videoPlayer = this.createPlayer(MediaItem.fromUri(Uri.fromFile(File(path))), null, currentTime)
            this.isShowDownloaded = true
            this.videoIsOriginal = true
            this.videoIsPlaying = true
            this.videoIsLoading = false
            this.startVideoTimer()
            return
        }
        if (this.fileProperty == null) {
            Toast.show("未获取到文件信息")
            return
        }
        val videoSource = if (isForceSmooth) this.fm.previewVideoSource else this.videoOnlineSource
        this.isShowDownloaded = false
        this.videoIsOriginal = videoSource.cacheKey == this.fm.downloadSource.cacheKey
        this.videoIsLoading = true

        //直接播放服务器上的文件,不经过SDK,所以不会产生缓存
        this.videoPlayer = this.createPlayer(MediaItem.fromUri(videoSource.url), videoSource.headers, currentTime)
    }

    /**
     * 创建播放器并开始播放
     * @param headers 访问服务器时的请求头,播放本地文件时为null
     * @param position 开始播放的位置(毫秒)
     */
    private fun createPlayer(mediaItem: MediaItem, headers: Map<String, String>?, position: Long): ExoPlayer {
        val context = DairoUI.context
        val builder = ExoPlayer.Builder(context)

            //播放视频时暂停其他App的声音
            .setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(), true)
        if (headers != null) {
            builder.setMediaSourceFactory(DefaultMediaSourceFactory(DefaultHttpDataSource.Factory().setDefaultRequestProperties(headers)))
        }
        val player = builder.build()

        //默认的跳转只会跳到附近的关键帧,关键帧间隔较大时跳转的时间不准,这里要求精确跳转
        player.setSeekParameters(SeekParameters.EXACT)
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {

                //加载期间又切换了清晰度或停止了播放,这个播放器已经不用了
                if (player !== videoPlayer) {
                    return
                }
                if (playbackState == Player.STATE_READY && videoIsLoading) {//视频缓冲完成
                    videoIsLoading = false
                    videoIsPlaying = true
                    startVideoTimer()
                } else if (playbackState == Player.STATE_ENDED) {
                    onVideoPlayEnd()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                if (player !== videoPlayer) {
                    return
                }
                val isDecoderError = error.errorCode in DECODER_ERROR_CODES

                //原画超出手机的解码能力(比如低端手机播放4K的HEVC视频),有流畅视频时从当前位置改播流畅视频,不改变清晰度设置
                if (isDecoderError && videoIsOriginal && hasPreviewVideo) {
                    Toast.show("这台手机不支持播放原画,已切换到流畅")
                    videoPlay(isForceSmooth = true)
                    return
                }
                stopVideoPlayer()
                videoIsLoading = false
                videoIsPlaying = false
                isVideoReplay = true//再次点击播放时重新加载
                videoIsPlayed = false//恢复显示缩略图
                Toast.show(if (isDecoderError) "这台手机不支持播放这个视频的格式" else "视频加载失败,请检查网络或访问权限")
            }
        })
        player.setMediaItem(mediaItem, position)
        player.setPlaybackSpeed(if (this.videoIsSpeedUp) VIDEO_SPEED_UP_RATE else 1f)
        player.playWhenReady = true
        player.prepare()
        return player
    }

    /** 服务器上有没有流畅视频 */
    private val hasPreviewVideo: Boolean
        get() = this.fileProperty?.extraList?.any { it.name == "preview" } == true

    /** 视频在线播放时的文件 */
    private val videoOnlineSource: FileSource
        get() {
            if (!this.hasPreviewVideo) {//没有预览视频，使用原视频
                return this.fm.downloadSource
            }
            return when (SettingStore.videoQuality) {
                SettingStore.VIDEO_QUALITY_ORIGINAL -> this.fm.downloadSource//播放原文件视频
                SettingStore.VIDEO_QUALITY_SMOOTH -> this.fm.previewVideoSource//播放流畅视频
                else -> if (SettingStore.isLanServer) this.fm.downloadSource else this.fm.previewVideoSource//自动:局域网优先原画
            }
        }

    /** 手指按下时停止回弹动画 */
    fun onTouchDown() {
        this.animationJob?.cancel()
    }

    /**
     * 手指拖动,图片放大时优先移动图片,到达图片边界之后带动页面移动。超出边界的方向有阻力
     * @param delta 手指移动的距离
     */
    fun onImageDrag(delta: Offset) {
        val pageOffset = this.pageOffset
        val maxImageOffset = max((this.displayW1 * this.zoomAmount - this.screenWidth) / 2, 0f)

        //页面已经被带动时,反方向拖动要先把页面拖回来
        val oldPageMove = this.hStackOffset - pageOffset
        val width = this.currentOffsetPosition.x + oldPageMove + delta.x
        val imageOffset = min(max(width, -maxImageOffset), maxImageOffset)
        var pageMove = width - imageOffset

        //第一张和最后一张没有可以切换的页面
        val isNoPage = (pageMove > 0 && this.currentIndex == 0) || (pageMove < 0 && this.currentIndex == this.fileModels.size - 1)
        if (isNoPage && abs(pageMove) > abs(oldPageMove)) {
            pageMove = oldPageMove + (pageMove - oldPageMove) * this.rubberBandRate(abs(oldPageMove), this.screenWidth)
        }
        this.hStackOffset = pageOffset + pageMove

        var height = this.currentOffsetPosition.y
        if (this.zoomAmount > 1) {//图片放大之后才能上下移动
            val maxHeight = max((this.displayH1 * this.zoomAmount - this.screenHeight) / 2, 0f)
            val direction = if (delta.y > 0) 1f else -1f

            //到达边界之前的距离没有阻力,超出边界的部分有阻力
            val distance = abs(delta.y)
            val free = min(max(maxHeight - height * direction, 0f), distance)
            val overflow = max(height * direction - maxHeight, 0f)
            height += direction * (free + (distance - free) * this.rubberBandRate(overflow, this.screenHeight))
        }
        this.currentOffsetPosition = Offset(imageOffset, height)
    }

    /**
     * 超出边界时手指移动和内容移动的比例,超出越多阻力越大
     * @param overflow 已经超出边界的距离
     * @param size 屏幕尺寸,内容最多超出这个距离
     */
    private fun rubberBandRate(overflow: Float, size: Float): Float {
        val rate = 1 - min(overflow / size, 1f)
        return RUBBER_BAND_RATE * rate * rate
    }

    /**
     * 拖动结束,决定切换页面、回弹或者惯性滑动
     * @param velocity 松手时手指的速度
     */
    fun onImageDragEnd(velocity: Velocity) {

        //页面被带动时说明图片已经到达边界,不需要惯性滑动
        val isPageMoved = this.hStackOffset != this.pageOffset
        this.withAnimation {
            this.fixCropImage(velocity.x)
        }
        if (!isPageMoved) {
            this.startInertia(Offset(velocity.x, velocity.y))
        }
    }

    /** 开始惯性滑动,图片放大之后才有 */
    private fun startInertia(velocity: Offset) {
        this.stopInertia()
        if (this.zoomAmount <= 1 || hypot(velocity.x, velocity.y) < INERTIA_MIN_VELOCITY * this.density) {
            return
        }
        this.inertiaVelocity = velocity
        var lastTime = 0L
        val callback = object : Choreographer.FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                if (inertiaCallback !== this) {
                    return
                }
                if (lastTime > 0) {
                    stepInertia((frameTimeNanos - lastTime) / 1_000_000_000.0)
                }
                lastTime = frameTimeNanos
                if (inertiaCallback === this) {
                    Choreographer.getInstance().postFrameCallback(this)
                }
            }
        }
        this.inertiaCallback = callback
        Choreographer.getInstance().postFrameCallback(callback)
    }

    /** 停止惯性滑动 */
    fun stopInertia() {
        this.inertiaCallback?.let { Choreographer.getInstance().removeFrameCallback(it) }
        this.inertiaCallback = null
    }

    /**
     * 手指按下时停止惯性滑动,正在弹回时要回到边界内
     */
    fun stopInertiaByTouch() {
        this.stopInertia()
        this.withAnimation {
            this.fixCropImage()
        }
    }

    /**
     * 惯性滑动一帧
     * @param time 距离上一帧的时间(秒)
     */
    private fun stepInertia(time: Double) {
        val maxWidth = max((this.displayW1 * this.zoomAmount - this.screenWidth) / 2, 0f)
        val maxHeight = max((this.displayH1 * this.zoomAmount - this.screenHeight) / 2, 0f)
        val x = this.stepInertia(this.currentOffsetPosition.x, this.inertiaVelocity.x, maxWidth, time)
        val y = this.stepInertia(this.currentOffsetPosition.y, this.inertiaVelocity.y, maxHeight, time)
        this.currentOffsetPosition = Offset(x.first, y.first)
        this.inertiaVelocity = Offset(x.second, y.second)
        if (x.second == 0f && y.second == 0f) {
            this.stopInertia()
        }
    }

    /**
     * 计算一个方向的惯性滑动。边界内速度逐渐衰减,超出边界后由弹簧拉回。这个方向结束时速度为0
     * @return 新的位置和速度
     */
    private fun stepInertia(offset: Float, velocity: Float, maxOffset: Float, time: Double): Pair<Float, Float> {
        val stopVelocity = INERTIA_STOP_VELOCITY * this.density
        val overflow = offset - min(max(offset, -maxOffset), maxOffset)
        if (overflow == 0f) {
            val newVelocity = velocity * INERTIA_DECAY.pow(time * 1000).toFloat()
            if (abs(newVelocity) < stopVelocity) {
                return Pair(offset, 0f)
            }
            return Pair(offset + newVelocity * time.toFloat(), newVelocity)
        }
        val acceleration = -BOUNCE_STIFFNESS * overflow - BOUNCE_DAMPING * velocity
        val newVelocity = velocity + acceleration * time.toFloat()
        val newOffset = offset + newVelocity * time.toFloat()
        val bound = if (overflow > 0) maxOffset else -maxOffset
        val isBack = if (overflow > 0) newOffset <= bound else newOffset >= bound
        if (isBack || (abs(overflow) < 0.5f && abs(newVelocity) < stopVelocity)) {
            return Pair(bound, 0f)
        }
        return Pair(newOffset, newVelocity)
    }

    /**
     * 双指缩放,以双指中心点为中心缩放,同时跟随中心点移动
     * @param scale 相对上一次的缩放比例
     * @param center 双指中心点在屏幕上的位置
     * @param translation 相对上一次中心点移动的距离
     */
    fun onImagePinch(scale: Float, center: Offset, translation: Offset) {
        val centerX = center.x - this.screenWidth / 2
        val centerY = center.y - this.screenHeight / 2
        this.zoomAmount *= scale

        //先跟随手指移动,再缩放,缩放之后中心点下的图片内容位置不变
        val width = this.currentOffsetPosition.x + translation.x
        val height = this.currentOffsetPosition.y + translation.y
        this.currentOffsetPosition = Offset(centerX - (centerX - width) * scale, centerY - (centerY - height) * scale)
    }

    /** 双指缩放结束,超出范围时回弹 */
    fun onImagePinchEnd(center: Offset) {
        this.withAnimation {
            if (this.zoomAmount > 8f) {//放大倍数不能大于8倍
                this.onImagePinch(8f / this.zoomAmount, center, Offset.Zero)
            }
            this.fixCropImage()
        }
    }

    /**
     * 修复裁剪范围
     * @param velocity 松手时手指的水平速度,向右为正
     */
    fun fixCropImage(velocity: Float = 0f) {
        if (this.zoomAmount < 1) {//手动缩小比例不能小于最小缩小比例
            this.zoomAmount = 1f
        }

        //当前偏移
        var offset = this.currentOffsetPosition
        if (this.displayW1 * this.zoomAmount < this.screenWidth) {//如果缩放后的宽小于屏幕宽度,则将图片屏幕水平居中(x轴偏移0)
            offset = Offset(0f, offset.y)
        } else {
            val offsetMinWidth = (this.displayW1 * this.zoomAmount - this.screenWidth) / 2
            offset = Offset(min(max(offset.x, -offsetMinWidth), offsetMinWidth), offset.y)
        }
        if (this.displayH1 * this.zoomAmount < this.screenHeight) {//如果缩放后的高小于屏幕高度,则将图片屏幕垂直居中(y轴偏移0)
            offset = Offset(offset.x, 0f)
        } else {

            //拖动到边界计算
            val offsetMinHeight = (this.displayH1 * this.zoomAmount - this.screenHeight) / 2
            offset = Offset(offset.x, min(max(offset.y, -offsetMinHeight), offsetMinHeight))
        }
        this.currentOffsetPosition = offset
        this.fixPageViewPosition(velocity)
    }

    /** 删除按钮点击事件 */
    fun onDeleteClick() {
        FilesApi.delete(listOf(this.fm.id)).send {

            //当前显示的索引
            var currentIndex = this.currentIndex

            //移除当前对象
            this.fileModels = this.fileModels.filterIndexed { i, _ -> i != currentIndex }

            //通知相册列表页面更新
            NotificationCenter.post(AlbumPage.ALBUM_PAGE_RELOAD_DATA)
            Toast.show("删除成功")
            if (this.fileModels.isEmpty()) {
                this.onEmpty()
                return@send
            }
            if (currentIndex >= this.fileModels.size) {//如果当前序号超出了范围
                currentIndex = this.fileModels.size - 1
            }
            this.changePage(currentIndex)
        }
    }

    /** 文件全部删除之后关闭页面 */
    var onEmpty: () -> Unit = {}

    /**
     * 仅下载点击事件
     * @param isSaveToAlbum 下载完成之后是否保存到相册
     */
    fun onDownloadOnlyClick(isSaveToAlbum: Boolean = false) {
        if (isSaveToAlbum && !MediaStoreUtil.checkWritePermission()) {
            Toast.show("请允许存储权限后再保存")
            return
        }
        this.isSaveToAlbum = isSaveToAlbum
        this.downloadTip = ""

        //添加下载,如果文件在缓存中,则将缓存标记为永久保存
        try {
            FileSdkUtil.download(this.fm.downloadSource)
        } catch (e: Throwable) {
            this.downloadFinish(e.fileSdkMessage)
            return
        }
        if (FileSdkUtil.cachedPath(this.fm.downloadSource) != null) {//文件已经被下载
            this.downloadFinish()
        }
    }

    /** SDK事件,更新当前文件的下载进度 */
    fun onFileSdkEvent(event: Event) {
        if (!this.isDownloading) {
            return
        }
        val id = FileSdkUtil.cacheId(this.fm.downloadSource) ?: return
        when (event) {
            is Event.Progress -> {
                val size = event.size
                if (event.id == id && size != null && size > 0UL) {
                    this.downloadTip = "${(event.downloaded.toDouble() / size.toDouble() * 100).toInt()}%"
                }
            }

            is Event.StateChanged -> {
                if (event.id != id) {
                    return
                }
                if (event.state == DownloadState.FINISH) {
                    this.downloadFinish()
                } else if (event.state == DownloadState.ERROR) {
                    this.downloadFinish(FileSdkUtil.friendlyError(event.error ?: "下载失败"))
                }
            }

            else -> {}
        }
    }

    /**
     * 文件下载完成后的操作
     * @param errorMsg 下载失败时的错误信息
     */
    private fun downloadFinish(errorMsg: String = "") {
        val path = FileSdkUtil.cachedPath(this.fm.downloadSource)
        if (path != null) {//文件不一定下载成功,所以这里要加判断
            if (this.isSaveToAlbum) {// 下载完成之后保存到相册
                this.saveToAlbum(this.fm, path)
            } else {
                Toast.show("下载完成")
            }
            if (this.isVideo && !this.isShowDownloaded) {//当前显示的视频并非已下载版
                this.isVideoReplay = true
                if (this.videoIsPlaying) {//如果当前视频正在播放中,则重新播放已经下载的版本
                    this.onVideoPlayClick()
                }
            }
        } else {
            Toast.show("下载失败:$errorMsg")
        }

        //取消下载模式
        this.downloadTip = null
        this.isSaveToAlbum = false
    }

    /** 保存到系统相册,实况照片只保存照片部分 */
    private fun saveToAlbum(fm: FileModel, path: String) {
        this.scope.launch {
            val error = withContext(Dispatchers.IO) {
                runCatching {
                    if (fm.isDlive) {
                        val info = DliveUtil.getInfo(path) ?: throw IllegalStateException("实况照片格式不正确")
                        MediaStoreUtil.save(fm.name.substringBeforeLast('.') + "." + info.photoExt, false) { ByteArrayInputStream(info.photoData) }
                    } else {
                        MediaStoreUtil.save(fm.name, fm.isVideo) { File(path).inputStream() }
                    }
                }.exceptionOrNull()
            }
            if (error == null) {
                Toast.show("已保存到相册")
            } else {
                Toast.show("保存相册失败: ${error.message ?: "未知错误"}")
            }
        }
    }

    /** 加载原图点击事件 */
    fun onLoadOriginalClick() {

        //请求加载原图
        this.loadOriginal()
    }

    /** 页面关闭时停止播放和下载 */
    override fun onCleared() {
        this.stopInertia()
        this.cancelFetch()
        this.propertyApiHttp?.cancel()
        this.stopVideoPlayer()
        this.dliveVm.clear()
        super.onCleared()
    }
}
