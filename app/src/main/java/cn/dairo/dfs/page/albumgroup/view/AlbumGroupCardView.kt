package cn.dairo.dfs.page.albumgroup.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cn.dairo.dfs.api.model.FileModel
import cn.dairo.dfs.components.DfsImage
import cn.dairo.dfs.components.ImagePlaceholder
import cn.dairo.dfs.extension.canThumbOnline
import cn.dairo.dfs.extension.placeholderIcon
import cn.dairo.dfs.extension.primaryText
import cn.dairo.dfs.extension.thumbOnlineSource
import cn.dairo.dfs.extension.thumbSource
import cn.dairo.dfs.page.album.view.TEXT_SHADOW
import cn.dairo.dfs.page.albumgroup.entity.AlbumGroup
import cn.dairo.dfs.page.albumgroup.entity.AlbumGroupPhoto

/** 照片之间的缝隙 */
private val GAP = 4.dp

/**
 * 一个分组:上面是标题,下面是照片
 * @param width 整个分组的宽度
 * @param onClick 照片点击事件
 */
@Composable
fun AlbumGroupCardView(group: AlbumGroup, width: Dp, onClick: (FileModel) -> Unit) {

    //一行放3张正方形照片时,每张照片的边长
    val smallSize = (width - GAP * 2) / 3
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(group.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = Color.primaryText, modifier = Modifier.padding(start = 4.dp))

        //根据照片数量选择排列方式
        val photos = group.photos
        when (photos.size) {
            1 -> Photo(photos[0], width, width * 0.6f, onClick)
            2 -> {//左右各一张
                val size = (width - GAP) / 2
                Row(horizontalArrangement = Arrangement.spacedBy(GAP)) {
                    Photo(photos[0], size, size, onClick)
                    Photo(photos[1], size, size, onClick)
                }
            }

            3 -> {//左边一张,右边两张,右边两张加起来和左边一样高
                val bigSize = smallSize * 2 + GAP
                Row(horizontalArrangement = Arrangement.spacedBy(GAP)) {
                    Photo(photos[0], bigSize, bigSize, onClick)
                    Column(verticalArrangement = Arrangement.spacedBy(GAP)) {
                        Photo(photos[1], smallSize, smallSize, onClick)
                        Photo(photos[2], smallSize, smallSize, onClick)
                    }
                }
            }

            else -> {//第一行一张,第二行三张
                Column(verticalArrangement = Arrangement.spacedBy(GAP)) {
                    Photo(photos[0], width, width * 0.6f, onClick)
                    Row(horizontalArrangement = Arrangement.spacedBy(GAP)) {
                        Photo(photos[1], smallSize, smallSize, onClick)
                        Photo(photos[2], smallSize, smallSize, onClick)
                        Photo(photos[3], smallSize, smallSize, onClick)
                    }
                }
            }
        }
    }
}

/** 一张照片,左上角显示拍摄日期 */
@Composable
private fun Photo(photo: AlbumGroupPhoto, width: Dp, height: Dp, onClick: (FileModel) -> Unit) {
    val fm = photo.fm
    Box(Modifier.size(width, height).clip(RoundedCornerShape(10.dp)).clickable { onClick(fm) }) {
        if (fm.canThumbOnline) {

            //按实际像素请求在线生成的图片,这样不会模糊也不会有锯齿,生成期间先显示缩略图
            //没有缩略图的普通图片,服务器直接用原图生成
            val (w, h) = with(LocalDensity.current) { width.toPx() to height.toPx() }
            DfsImage(fm.thumbOnlineSource(w, h), Modifier.fillMaxSize(), fallback = if (fm.hasThumb) fm.thumbSource else null, placeholder = fm.placeholderIcon)
        } else {//服务器还没有生成缩略图,或者账号关闭了生成缩略图
            ImagePlaceholder(fm.placeholderIcon, Modifier.fillMaxSize())
        }
        Text(
            photo.dateLabel,
            style = MaterialTheme.typography.bodyMedium.copy(shadow = TEXT_SHADOW),
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            modifier = Modifier.padding(6.dp),
        )
    }
}
