package cn.dairo.dfs.page.trash.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Image
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import cn.dairo.dfs.api.model.FileModel
import cn.dairo.dfs.components.DfsImage
import cn.dairo.dfs.extension.canThumbOnline
import cn.dairo.dfs.extension.primaryText
import cn.dairo.dfs.extension.secondaryText
import cn.dairo.dfs.extension.thumbOnlineSource
import cn.dairo.dfs.page.file.view.UnknownFileIcon
import cn.dairo.dfs.page.trash.entity.TrashEntity
import cn.dairo.dfs.util.FileSource
import cn.dairo.ui.util.fileSize

@Composable
fun TrashItemView(entity: TrashEntity, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 6.dp), verticalAlignment = Alignment.CenterVertically) {

        //缩略图,回收站里的文件也能请求在线生成的图片
        val fm = entity.fm.let { FileModel(id = it.id, name = it.name, size = it.size, fileFlag = it.fileFlag, date = 0, hasThumb = it.hasThumb) }
        when {
            fm.hasThumb -> DfsImage(FileSource.thumb(fm.id), Modifier.size(44.dp).clip(RoundedCornerShape(6.dp)), placeholder = Icons.Default.Image)

            //账号关闭了生成缩略图时,普通图片用原图在线生成
            fm.canThumbOnline -> {
                val px = with(LocalDensity.current) { 44.dp.toPx() }
                DfsImage(fm.thumbOnlineSource(px, px), Modifier.size(44.dp).clip(RoundedCornerShape(6.dp)), placeholder = Icons.Default.Image)
            }

            else -> UnknownFileIcon()
        }
        Column(Modifier.weight(1f).padding(start = 10.dp)) {
            Text(entity.fm.name, style = MaterialTheme.typography.bodyLarge, color = Color.primaryText)
            Text(entity.fm.date + "    " + entity.fm.size.fileSize, style = MaterialTheme.typography.bodyMedium, color = Color.secondaryText)
            HorizontalDivider(Modifier.padding(top = 6.dp))
        }
        Icon(
            if (entity.isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = Color.primaryText,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}
