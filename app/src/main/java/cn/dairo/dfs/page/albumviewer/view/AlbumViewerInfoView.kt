package cn.dairo.dfs.page.albumviewer.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cn.dairo.dfs.api.model.FilePropertyModel
import cn.dairo.dfs.components.AppSheet
import cn.dairo.dfs.extension.primaryText
import cn.dairo.dfs.extension.secondaryBackground
import cn.dairo.dfs.extension.secondaryText
import cn.dairo.dfs.page.albumviewer.viewmodel.AlbumViewerViewModel
import cn.dairo.ui.util.format
import java.util.Date
import cn.dairo.ui.util.timeFormat
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.longOrNull
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Locale

/**
 * 文件属性JSON里的图片、视频信息,没有的字段服务端不会返回
 */
class AlbumViewerMediaInfo private constructor(private val json: JsonObject) {

    companion object {

        /** 解析属性JSON,格式不正确时返回null */
        fun parse(property: String): AlbumViewerMediaInfo? {
            return runCatching { AlbumViewerMediaInfo(Json.parseToJsonElement(property).jsonObject) }.getOrNull()
        }
    }

    private fun string(key: String): String? = (this.json[key] as? JsonPrimitive)?.content?.takeIf { it.isNotEmpty() }
    private fun long(key: String): Long? = (this.json[key] as? JsonPrimitive)?.longOrNull
    private fun double(key: String): Double? = (this.json[key] as? JsonPrimitive)?.doubleOrNull

    val width get() = this.long("width")
    val height get() = this.long("height")
    val date get() = this.long("date")
    val originalDate get() = this.long("originalDate")
    private val make get() = this.string("make")
    private val camera get() = this.string("camera")
    val lens get() = this.string("lens")
    private val focalLength get() = this.string("focalLength")
    private val focalLength35 get() = this.long("focalLength35")
    private val fNumber get() = this.string("fNumber")
    private val shutterSpeed get() = this.string("shutterSpeed")
    private val iso get() = this.long("iso")
    private val lat get() = this.double("lat")
    private val long get() = this.double("long")
    private val orientation get() = this.long("orientation")
    private val fps get() = this.double("fps")
    private val bitrate get() = this.long("bitrate")
    private val duration get() = this.long("duration")

    /** 设备名称,型号一般已经能说明设备,没有型号时才显示制造商 */
    val device: String?
        get() = this.camera ?: this.make

    /** 焦距,优先显示等效35mm焦距 */
    private val focalLabel: String?
        get() {
            this.focalLength35?.let { return "$it mm" }
            return this.focalLength?.let { "$it mm" }
        }

    /** 尺寸,照片要旋转90度显示时宽高调换 */
    val sizeLabel: String?
        get() {
            val width = this.width ?: return null
            val height = this.height ?: return null
            return if (this.orientation?.let { it in 5L..8L } == true) "$height × $width" else "$width × $height"
        }

    /** 像素数 */
    val megapixelLabel: String?
        get() {
            val width = this.width ?: return null
            val height = this.height ?: return null
            return String.format(Locale.ROOT, "%.1f MP", width * height / 1_000_000.0)
        }

    /** 帧率 */
    val fpsLabel: String?
        get() = this.fps?.let { String.format(Locale.ROOT, "%s fps", if (it % 1.0 == 0.0) it.toLong().toString() else it.toString()) }

    /** 视频比特率 */
    private val bitrateLabel: String?
        get() {
            val bitrate = this.bitrate ?: return null
            return if (bitrate >= 1_000_000) String.format(Locale.ROOT, "%.1f Mbps", bitrate / 1_000_000.0) else "${bitrate / 1000} kbps"
        }

    /** 坐标 */
    val locationLabel: String?
        get() {
            val lat = this.lat ?: return null
            val long = this.long ?: return null
            return String.format(Locale.ROOT, "%.5f, %.5f", lat, long)
        }

    /** 拍摄参数,照片是ISO、焦距、光圈、快门,视频是时长、比特率 */
    val parameters: List<String>
        get() = listOfNotNull(
            this.iso?.let { "ISO $it" },
            this.focalLabel,
            this.fNumber?.let { "ƒ$it" },
            this.shutterSpeed?.let { "$it s" },
            this.duration?.timeFormat,
            this.bitrateLabel,
        )
}

/**
 * 属性面板,先显示一半高度,可以上拉到全屏
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumViewerInfoView(vm: AlbumViewerViewModel) {
    if (!vm.showInfo) {
        return
    }

    /** 修改拍摄时间页面是否显示 */
    var showCaptureTime by remember { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = { vm.showInfo = false }, sheetState = rememberBottomSheetState(SheetValue.Hidden)) {
        val property = vm.fileProperty
        when {
            property != null -> Content(vm, property) { showCaptureTime = true }
            vm.propertyIsLoading -> Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            else -> Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                TextButton(onClick = vm::onInfoClick) {
                    Text("加载失败，点击重试")
                }
            }
        }
    }
    val property = vm.fileProperty
    if (showCaptureTime && property != null) {
        val info = AlbumViewerMediaInfo.parse(property.property)
        AlbumViewerCaptureTimeView(
            vm,
            date = info?.date?.takeIf { it > 0 } ?: property.uploadDate,
            isModified = info?.originalDate != null,
        ) {
            showCaptureTime = false
        }
    }
}

@Composable
private fun Content(vm: AlbumViewerViewModel, property: FilePropertyModel, onAdjustClick: () -> Unit) {
    val info = AlbumViewerMediaInfo.parse(property.property)
    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 16.dp).navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(property.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = Color.primaryText)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(vm.fileTime ?: "", style = MaterialTheme.typography.bodyMedium, color = Color.secondaryText, modifier = Modifier.weight(1f))
                TextButton(onClick = onAdjustClick) {
                    Text("调整")
                }
            }
        }
        MediaCard(vm, property, info)
        Column(
            Modifier.fillMaxWidth().background(Color.secondaryBackground, RoundedCornerShape(12.dp)).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            InfoRow("文件路径", property.path)
            info?.locationLabel?.let { InfoRow("拍摄位置", it) }
            if (property.uploadDate > 0) {
                InfoRow("上传时间", Date(property.uploadDate).format())
            }
        }
    }
}

/** 设备、尺寸和拍摄参数 */
@Composable
private fun MediaCard(vm: AlbumViewerViewModel, property: FilePropertyModel, info: AlbumViewerMediaInfo?) {
    val labels = listOfNotNull(info?.sizeLabel, if (vm.isVideo) info?.fpsLabel else info?.megapixelLabel, property.size)
    val parameters = info?.parameters ?: emptyList()
    Column(
        Modifier.fillMaxWidth().background(Color.secondaryBackground, RoundedCornerShape(12.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        info?.device?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = Color.primaryText)
        }
        info?.lens?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = Color.primaryText)
        }
        Text(labels.joinToString(" · "), style = MaterialTheme.typography.bodyMedium, color = Color.secondaryText)
        if (parameters.isNotEmpty()) {
            HorizontalDivider()
            Row {
                parameters.forEach {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = Color.secondaryText, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = Color.secondaryText)
        SelectionContainer {
            Text(value, style = MaterialTheme.typography.bodyMedium, color = Color.primaryText)
        }
    }
}

/**
 * 修改拍摄时间页面,只修改服务器上记录的时间,不修改文件本身
 * @param date 当前拍摄时间(毫秒)
 * @param isModified 拍摄时间是否被修改过
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlbumViewerCaptureTimeView(vm: AlbumViewerViewModel, date: Long, isModified: Boolean, onDismiss: () -> Unit) {
    val zone = ZoneId.systemDefault()
    val original = remember(date) { LocalDateTime.ofInstant(Instant.ofEpochMilli(date), zone).withSecond(0).withNano(0) }

    //日期选择器使用UTC时间的0点
    val dateState = rememberDatePickerState(initialSelectedDateMillis = original.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
    val timeState = rememberTimePickerState(initialHour = original.hour, initialMinute = original.minute, is24Hour = true)

    /** 是否正在保存 */
    var isSaving by remember { mutableStateOf(false) }

    /** 保存失败的错误信息,面板盖在页面上面,Toast会被挡住 */
    var errorMsg by remember { mutableStateOf<String?>(null) }

    /** 当前选择的时间 */
    val selected = dateState.selectedDateMillis?.let {
        Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().atTime(timeState.hour, timeState.minute)
    }

    /**
     * 保存拍摄时间
     * @param millis 拍摄时间(毫秒),0代表恢复成照片本身的拍摄时间
     */
    val save = { millis: Long ->
        isSaving = true
        errorMsg = null
        vm.updateCaptureTime(millis) {
            isSaving = false
            if (it != null) {
                errorMsg = it
            } else {
                onDismiss()
            }
        }
    }
    AppSheet(onDismiss = { if (!isSaving) onDismiss() }) {
        Column(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onDismiss, enabled = !isSaving) {
                    Text("取消")
                }
                Text("调整日期与时间", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, color = Color.primaryText, modifier = Modifier.weight(1f))
                Box(Modifier.size(64.dp, 48.dp), contentAlignment = Alignment.Center) {
                    if (isSaving) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        TextButton(onClick = { selected?.let { save(it.atZone(zone).toInstant().toEpochMilli()) } }, enabled = selected != null && selected != original) {
                            Text("保存")
                        }
                    }
                }
            }
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
                DatePicker(dateState, title = null, headline = null, showModeToggle = false)
                TimeInput(timeState)
                errorMsg?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = Color.Red, modifier = Modifier.padding(8.dp))
                }
                if (isModified) {
                    TextButton(onClick = { save(0) }, enabled = !isSaving) {
                        Text("恢复原始时间")
                    }
                }
            }
        }
    }
}
