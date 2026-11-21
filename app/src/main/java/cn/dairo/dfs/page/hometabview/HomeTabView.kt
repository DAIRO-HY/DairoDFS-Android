package cn.dairo.dfs.page.hometabview

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import cn.dairo.dfs.components.liquidGlassBackground
import cn.dairo.dfs.components.relaunch
import cn.dairo.dfs.extension.primaryText
import cn.dairo.dfs.extension.theme
import cn.dairo.dfs.page.file.view.FileAddView
import cn.dairo.dfs.page.file.FilePage
import cn.dairo.dfs.page.mine.MinePage
import cn.dairo.dfs.util.NotificationCenter

/**
 * tab页面标记
 */
enum class TabPage {
    FILE_PAGE,//文件页面
    MINE_PAGE,//我的页面
}

/**
 * 首页底部的Tab按钮,中间是添加按钮
 * @param pageTag 当前选中的页面标记
 */
@Composable
fun HomeTabView(pageTag: TabPage, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().navigationBarsPadding(), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .widthIn(max = 420.dp)
                .padding(horizontal = 16.dp)
                .padding(bottom = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .liquidGlassBackground(RoundedCornerShape(30.dp))
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TabButton("文件", Icons.Default.Folder, pageTag == TabPage.FILE_PAGE) {
                    relaunch { FilePage() }
                }
                Spacer(Modifier.width(60.dp))
                TabButton("我的", Icons.Default.Person, pageTag == TabPage.MINE_PAGE) {
                    relaunch { MinePage() }
                }
            }

            //添加按钮
            Box(
                Modifier
                    .size(48.dp)
                    .liquidGlassBackground(CircleShape, Color.theme)
                    .clickable {

                        //显示添加视图
                        NotificationCenter.post(FileAddView.FILE_ADD_VIEW_SHOW_FLAG)
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Add, contentDescription = "添加", tint = Color.White)
            }
        }
    }
}

@Composable
private fun RowScope.TabButton(text: String, icon: ImageVector, isSelected: Boolean, action: () -> Unit) {
    val shape = RoundedCornerShape(26.dp)
    Column(
        Modifier
            .weight(1f)
            .height(52.dp)
            .clip(shape)
            .background(if (isSelected) Color.primaryText.copy(alpha = 0.12f) else Color.Transparent, shape)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                if (!isSelected) {
                    action()
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = null, tint = Color.primaryText, modifier = Modifier.size(22.dp))
        Text(text, style = MaterialTheme.typography.labelSmall, color = Color.primaryText, modifier = Modifier.padding(top = 3.dp))
    }
}
