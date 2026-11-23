package cn.dairo.dfs.page.sharefolder

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cn.dairo.dfs.components.AlertButton
import cn.dairo.dfs.components.ButtonRole
import cn.dairo.dfs.components.NavigationPage
import cn.dairo.dfs.components.TextFieldAlert
import cn.dairo.dfs.extension.primaryText
import cn.dairo.dfs.extension.secondaryBackground
import cn.dairo.dfs.extension.secondaryText
import cn.dairo.dfs.extension.tertiaryText
import cn.dairo.dfs.extension.theme
import cn.dairo.ui.setting.settingColors

/**
 * 从其他App分享文件时,选择上传到的文件夹,对应iOS的ShareFolderView
 */
@Composable
fun ShareFolderPage(vm: ShareFolderViewModel, onCancel: () -> Unit) {
    val isLogin = vm.account != null

    //返回键:先回到上一级文件夹,根目录时关闭分享页面
    BackHandler {
        if (isLogin && vm.folder.isNotEmpty()) {
            vm.open(vm.parentFolder)
        } else {
            onCancel()
        }
    }
    NavigationPage(
        title = vm.title,
        navigationIcon = {
            TextButton(onClick = onCancel) {
                Text("取消", color = settingColors.accent)
            }
        },
        actions = {
            IconButton(
                onClick = {
                    vm.newFolderName = ""
                    vm.isShowCreateFolder = true
                },
                enabled = isLogin && !vm.isLoading && vm.loadError == null,
            ) {
                Icon(Icons.Default.CreateNewFolder, contentDescription = "新建文件夹")
            }
        },
    ) {
        Box(Modifier.fillMaxWidth().weight(1f)) {
            when {
                !isLogin -> Message("请先打开DairoDFS登录")
                vm.loadError != null -> ErrorView(vm)
                vm.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                else -> FolderList(vm)
            }
        }
        if (isLogin) {
            BottomBar(vm)
        }
    }
    TextFieldAlert(
        isPresented = vm.isShowCreateFolder,
        title = "新建文件夹",
        text = vm.newFolderName,
        onTextChange = { vm.newFolderName = it },
        placeholder = "文件夹名",
        onDismiss = { vm.isShowCreateFolder = false },
        buttons = listOf(
            AlertButton("创建") { vm.createFolder() },
            AlertButton("取消", ButtonRole.CANCEL),
        ),
    )
}

/** 子文件夹列表 */
@Composable
private fun FolderList(vm: ShareFolderViewModel) {
    if (vm.folders.isEmpty() && vm.folder.isEmpty()) {
        Message("没有文件夹,将上传到根目录")
        return
    }
    LazyColumn(Modifier.fillMaxSize()) {
        if (vm.folder.isNotEmpty()) {
            item {
                FolderRow("返回上一级", Icons.AutoMirrored.Filled.Undo, Color.theme, showArrow = false) {
                    vm.open(vm.parentFolder)
                }
            }
        }
        items(vm.folders) { name ->
            FolderRow(name, Icons.Default.Folder, Color(0xFF32ADE6), showArrow = true) {
                vm.open(vm.folder + "/" + name)
            }
        }
    }
}

/** 列表的一行 */
@Composable
private fun FolderRow(name: String, icon: ImageVector, iconColor: Color, showArrow: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(start = 16.dp, end = 16.dp, top = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(28.dp).padding(bottom = 10.dp))
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Row(Modifier.padding(bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.primaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (showArrow) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.tertiaryText)
                }
            }
            HorizontalDivider()
        }
    }
}

/** 加载失败,可以重试,不在根目录时可以回到根目录 */
@Composable
private fun ErrorView(vm: ShareFolderViewModel) {
    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(vm.loadError ?: "", color = Color.secondaryText, textAlign = TextAlign.Center)
        TextButton(onClick = { vm.load() }, modifier = Modifier.padding(top = 8.dp)) {
            Text("重试", color = settingColors.accent)
        }
        if (vm.folder.isNotEmpty()) {
            TextButton(onClick = { vm.open("") }) {
                Text("回到根目录", color = settingColors.accent)
            }
        }
    }
}

/** 底部显示上传位置和上传按钮 */
@Composable
private fun BottomBar(vm: ShareFolderViewModel) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Color.secondaryBackground)
            .navigationBarsPadding()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {

        //路径很长时保留最后面的文件夹名
        Text(
            "上传到:" + vm.folder.ifEmpty { "/" },
            style = MaterialTheme.typography.bodySmall,
            color = Color.secondaryText,
            maxLines = 1,
            overflow = TextOverflow.StartEllipsis,
        )
        Button(
            onClick = { vm.upload() },
            enabled = !vm.isLoading && vm.loadError == null,
            colors = ButtonDefaults.buttonColors(containerColor = settingColors.accent),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        ) {
            Text("上传到这里")
        }
    }
}

/** 页面中间的提示文字 */
@Composable
private fun Message(text: String) {
    Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
        Text(text, color = Color.secondaryText, textAlign = TextAlign.Center)
    }
}
