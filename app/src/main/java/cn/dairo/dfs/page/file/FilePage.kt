package cn.dairo.dfs.page.file

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import cn.dairo.dfs.components.AppSheet
import cn.dairo.dfs.extension.listBackground
import cn.dairo.dfs.page.albumviewer.AlbumViewerPage
import cn.dairo.dfs.page.audioplayer.AudioPlayerPage
import cn.dairo.dfs.page.file.view.FileAddView
import cn.dairo.dfs.page.file.view.FileOptionBarView
import cn.dairo.dfs.page.file.view.FileOptionView
import cn.dairo.dfs.page.file.view.FileView
import cn.dairo.dfs.page.file.viewmodel.FileViewModel
import cn.dairo.dfs.page.hometabview.HomeTabView
import cn.dairo.dfs.page.hometabview.TabPage
import cn.dairo.dfs.store.SettingStore
import cn.dairo.dfs.util.OnReceive
import cn.dairo.dfs.util.rememberStateObject
import cn.dairo.ui.setting.LocalSettingNavigator
import cn.dairo.ui.setting.SettingStack

@Composable
fun FilePage() {
    val vm = rememberStateObject { FileViewModel() }
    val density = LocalDensity.current

    //顶部和底部按钮的高度,列表上下留出这个位置
    var topHeight by remember { mutableIntStateOf(0) }
    var bottomHeight by remember { mutableIntStateOf(0) }
    OnReceive(FileAddView.FILE_ADD_VIEW_SHOW_FLAG) {
        vm.isSelectMode = false
        vm.isShowAddView = !vm.isShowAddView
    }
    SettingStack {

        //返回键:先退出选择模式和添加视图,再返回上一级文件夹;打开了其他页面时由导航栈处理
        val isTop = (LocalSettingNavigator.current?.depth ?: 0) == 0
        BackHandler(enabled = isTop && (vm.isSelectMode || vm.isShowAddView || SettingStore.lastOpenFolder.isNotEmpty())) {
            if (vm.isSelectMode) {
                vm.isSelectMode = false
                vm.clearSelected()
            } else if (vm.isShowAddView) {
                vm.isShowAddView = false
            } else {
                vm.onBackClick()
            }
        }
        Box(Modifier.fillMaxSize().background(Color.listBackground)) {
            with(density) {
                FileView(vm, PaddingValues(top = topHeight.toDp(), bottom = bottomHeight.toDp()))
            }

            //顶部和底部的按钮都浮在列表上面
            FileOptionBarView(vm, Modifier.align(Alignment.TopCenter).onSizeChanged { topHeight = it.height })
            Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().onSizeChanged { bottomHeight = it.height }) {
                FileOptionView(vm)
                FileAddView(vm)
                HomeTabView(TabPage.FILE_PAGE)
            }
        }
    }
    if (vm.showViewerPage) {
        AlbumViewerPage(remember { vm.getAlbumViewerViewModel() }) {
            vm.showViewerPage = false
        }
    }
    vm.audioFile?.let { audioFile ->
        AppSheet(onDismiss = { vm.audioFile = null }, heightFraction = 0.6f) {
            AudioPlayerPage(audioFile) {
                vm.audioFile = null
            }
        }
    }
}
