package cn.dairo.dfs.page.fileupload.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cn.dairo.dfs.extension.primaryText
import cn.dairo.dfs.extension.secondaryText
import cn.dairo.dfs.page.fileupload.FileUploadViewModel
import cn.dairo.dfs.util.FileSdkUtil
import cn.dairo.file.sdk.UploadInfo
import cn.dairo.file.sdk.UploadState
import cn.dairo.ui.root.Toast
import cn.dairo.ui.util.format
import java.util.Date
import cn.dairo.ui.util.fileSize
import kotlin.math.max

@Composable
fun FileUploadItemView(vm: FileUploadViewModel, item: UploadInfo) {

    //是否已经结束,跳过表示服务器上已经有相同的文件
    val isFinish = item.state == UploadState.FINISH || item.state == UploadState.SKIPPED
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            if (item.id in vm.checked) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = Color.primaryText,
            modifier = Modifier.size(22.dp).clickable { vm.onCheckClick(item.id) },
        )
        Column(Modifier.weight(1f).padding(start = 10.dp)) {
            Text(item.name, style = MaterialTheme.typography.bodyLarge, color = Color.primaryText)
            Row {
                Text(Date(item.createDate).format(), style = MaterialTheme.typography.bodySmall, color = Color.secondaryText, modifier = Modifier.weight(1f))
                item.size?.let {
                    Text(it.toLong().fileSize, style = MaterialTheme.typography.bodySmall, color = Color.secondaryText)
                }
            }
            if (isFinish) {
                Text(stateLabel(item.state), style = MaterialTheme.typography.bodySmall, color = Color.secondaryText)
            } else {
                val total = max((item.size ?: 0UL).toLong(), max(item.uploaded.toLong(), 1L))
                LinearProgressIndicator(progress = { item.uploaded.toFloat() / total }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val error = item.error?.let { FileSdkUtil.friendlyError(it) }
                    if (error != null) {
                        Text(
                            error,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Red,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f).clickable { Toast.show(error) },
                        )
                    } else {
                        Text(
                            vm.progressInfo[item.id] ?: item.uploaded.toLong().fileSize,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.secondaryText,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Text(stateLabel(item.state), style = MaterialTheme.typography.bodySmall, color = Color.secondaryText)
                    Icon(
                        if (item.state == UploadState.WAITING || item.state == UploadState.UPLOADING) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                        contentDescription = null,
                        tint = Color.secondaryText,
                        modifier = Modifier.padding(start = 4.dp).size(20.dp).clickable { vm.onStateClick(item) },
                    )
                }
            }
            HorizontalDivider(Modifier.padding(top = 4.dp))
        }
    }
}

/** 上传状态文字 */
private fun stateLabel(state: UploadState): String {
    return when (state) {
        UploadState.WAITING -> "等待上传"
        UploadState.UPLOADING -> "上传中"
        UploadState.PAUSED -> "已暂停"
        UploadState.ERROR -> "上传失败"
        UploadState.FINISH -> "上传完成"
        UploadState.SKIPPED -> "文件已存在"
    }
}
