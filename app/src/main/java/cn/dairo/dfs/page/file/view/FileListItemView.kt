package cn.dairo.dfs.page.file.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cn.dairo.dfs.components.DfsImage
import cn.dairo.dfs.extension.isAudio
import cn.dairo.dfs.extension.isFile
import cn.dairo.dfs.extension.placeholderIcon
import cn.dairo.dfs.extension.primaryText
import cn.dairo.dfs.extension.secondaryText
import cn.dairo.dfs.extension.thumbSource
import cn.dairo.dfs.page.file.entity.FileEntity
import cn.dairo.ui.util.format
import java.util.Date
import cn.dairo.ui.util.fileSize

@Composable
fun FileListItemView(item: FileEntity, isSelectMode: Boolean, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 6.dp), verticalAlignment = Alignment.CenterVertically) {

        //缩略图
        Thumb(item)
        Column(Modifier.weight(1f).padding(start = 10.dp)) {
            Text(item.fm.name, style = MaterialTheme.typography.bodyLarge, color = Color.primaryText)
            Text(
                Date(item.fm.date).format() + "    " + item.fm.size.fileSize,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.secondaryText,
            )
            HorizontalDivider(Modifier.padding(top = 6.dp))
        }
        if (isSelectMode) {//如果是选择模式
            Icon(
                if (item.isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = null,
                tint = Color.primaryText,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

/** 缩略图 */
@Composable
private fun Thumb(item: FileEntity) {
    val fm = item.fm
    when {
        !fm.isFile -> Icon(Icons.Default.Folder, contentDescription = null, tint = Color(0xFF32ADE6), modifier = Modifier.size(44.dp))

        //和查看页面用同一张缩略图,这样打开图片时不用走网络就能先显示缩略图
        fm.hasThumb -> DfsImage(fm.thumbSource, Modifier.size(44.dp).clip(RoundedCornerShape(6.dp)), placeholder = fm.placeholderIcon)

        fm.isAudio -> Box(
            Modifier.size(44.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xFFFF2D55)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color.White)
        }

        else -> UnknownFileIcon()
    }
}

/** 没有缩略图的文件 */
@Composable
fun UnknownFileIcon() {
    Icon(Icons.AutoMirrored.Filled.InsertDriveFile, contentDescription = null, tint = Color.secondaryText, modifier = Modifier.size(44.dp))
}
