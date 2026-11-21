package cn.dairo.dfs.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cn.dairo.dfs.extension.primaryText

/**
 * 浮在页面上的操作按钮,文字较短时是圆形,文字较长时自动变宽,一行里的按钮平分宽度
 * @param label 标题
 * @param icon 图标
 * @param disabled 是否禁用
 */
@Composable
fun RowScope.OptionButton(label: String, icon: ImageVector, disabled: Boolean = false, onClick: () -> Unit) {
    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
        Column(
            Modifier
                .height(50.dp)
                .defaultMinSize(minWidth = 50.dp)
                .liquidGlassBackground(RoundedCornerShape(25.dp))
                .clickable(enabled = !disabled, onClick = onClick)
                .padding(horizontal = 8.dp)
                .alpha(if (disabled) 0.4f else 1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(icon, contentDescription = null, tint = Color.primaryText, modifier = Modifier.size(20.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.primaryText,
                maxLines = 1,
                overflow = TextOverflow.Visible,
                modifier = Modifier.padding(top = 1.dp),
            )
        }
    }
}
