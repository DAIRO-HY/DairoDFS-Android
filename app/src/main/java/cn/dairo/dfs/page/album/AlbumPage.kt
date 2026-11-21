package cn.dairo.dfs.page.album

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import cn.dairo.dfs.components.OnPageAppear
import cn.dairo.dfs.extension.listBackground
import cn.dairo.dfs.page.album.view.AlbumGridView
import cn.dairo.dfs.page.album.view.AlbumOptionBarView
import cn.dairo.dfs.page.album.view.AlbumOptionView
import cn.dairo.dfs.page.album.viewmodel.AlbumViewModel
import cn.dairo.dfs.page.albumgroup.entity.AlbumViewMode
import cn.dairo.dfs.page.albumgroup.view.AlbumGroupView
import cn.dairo.dfs.page.albumgroup.view.AlbumModeBarView
import cn.dairo.dfs.page.albumviewer.AlbumViewerPage
import cn.dairo.dfs.page.systemalbum.SystemAlbumPage
import cn.dairo.dfs.page.systemalbum.viewmodel.UploadMode
import cn.dairo.dfs.util.OnReceive
import cn.dairo.dfs.util.rememberStateObject
import cn.dairo.ui.setting.LocalSettingNavigator
import cn.dairo.ui.setting.SettingStack

@Composable
fun AlbumPage() {
    val vm = rememberStateObject { AlbumViewModel() }

    //刷新数据通知
    OnReceive(AlbumPage.ALBUM_PAGE_RELOAD_DATA) {
        vm.isNeedReloadData = true
    }
    SettingStack {
        val navigator = LocalSettingNavigator.current

        //相册上传页面返回时,如果需要更新数据
        OnPageAppear(vm::reloadIfNeed)
        val onAddClick = {
            vm.isNeedReloadData = false
            navigator?.push { SystemAlbumPage(UploadMode.ALBUM) }
            Unit
        }
        BackHandler(enabled = vm.isSelectMode && (navigator?.depth ?: 0) == 0) {
            vm.isSelectMode = false
            vm.clearSelected()
        }
        Box(Modifier.fillMaxSize().background(Color.listBackground)) {

            //分组显示时没有操作按钮
            if (vm.viewMode == AlbumViewMode.ALL || vm.entityList.isEmpty()) {
                AlbumGridView(vm, onAddClick)
                AlbumOptionBarView(vm, Modifier.align(Alignment.TopEnd))
                AlbumOptionView(vm, onAddClick, Modifier.align(Alignment.BottomCenter))
            } else {

                //按年和按月的列表高度相差很大,这里要重新创建列表,否则会沿用上一个列表的滚动位置
                key(vm.viewMode) {
                    AlbumGroupView(if (vm.viewMode == AlbumViewMode.YEAR) vm.yearGroups else vm.monthGroups, vm)
                }
            }
            if (!vm.isSelectMode && vm.entityList.isNotEmpty()) {
                AlbumModeBarView(vm, Modifier.align(Alignment.BottomCenter))
            }
        }
    }
    if (vm.showViewerPage) {
        AlbumViewerPage(remember { vm.getAlbumViewerViewModel() }) {
            vm.showViewerPage = false

            //当相册查看页面关闭时,如果需要更新数据
            vm.reloadIfNeed()
        }
    }
}

object AlbumPage {

    /** 页面重新加载标识 */
    const val ALBUM_PAGE_RELOAD_DATA = "ALBUM_PAGE_RELOAD_DATA"
}
