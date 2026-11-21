package cn.dairo.dfs.page.albumviewer.view

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import cn.dairo.dfs.page.albumviewer.viewmodel.AlbumViewerViewModel

/**
 * 查看页面的手势操作视图:单击显示隐藏按钮,双击放大或者视频快进快退,长按视频倍速播放,拖动切换页面,双指缩放
 * 拖动和缩放处理的在前面,移动之后会消费事件,这样单击、双击就不会触发
 */
@Composable
fun AlbumViewerTouchView(vm: AlbumViewerViewModel, modifier: Modifier = Modifier) {
    Box(
        modifier
            .pointerInput(vm) {
                detectTapGestures(
                    onPress = {

                        //手指按下时停止惯性滑动,这次点击只用来停止,不算单击
                        if (vm.isInertiaRunning) {
                            vm.stopInertiaByTouch()
                        }
                        tryAwaitRelease()
                        if (vm.videoIsSpeedUp) {
                            vm.onVideoSpeedUpChange(false)
                        }
                    },
                    onTap = {
                        vm.showActionView = !vm.showActionView
                    },
                    onDoubleTap = { point ->
                        if (vm.isVideo) {
                            val isLeft = point.x < size.width / 2
                            vm.onVideoSeek(if (isLeft) -AlbumViewerViewModel.VIDEO_SEEK_SECONDS else AlbumViewerViewModel.VIDEO_SEEK_SECONDS)
                        } else {
                            vm.doubleClick(point)
                        }
                    },
                    onLongPress = {

                        //只在视频播放时生效
                        if (vm.videoIsPlaying) {
                            vm.onVideoSpeedUpChange(true)
                        }
                    },
                )
            }
            .pointerInput(vm) {

                //移动很小的距离就开始拖动
                val threshold = 2.dp.toPx()

                //移动超过这个距离之后不再算单击
                val tapMaxMove = 10.dp.toPx()
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    vm.onTouchDown()
                    val velocityTracker = VelocityTracker()
                    velocityTracker.addPosition(down.uptimeMillis, down.position)
                    val start = down.position
                    var lastPosition = start
                    var isDragging = false
                    var isPinching = false

                    //上一次双指的中心点,只剩一个手指时为null,这时不移动图片,否则中心点变化会让图片跳动
                    var lastCentroid: Offset? = null
                    while (true) {
                        val event = awaitPointerEvent()
                        val pressed = event.changes.filter { it.pressed }
                        if (pressed.isEmpty()) {
                            break
                        }
                        if (pressed.size >= 2) {
                            if (!vm.isImage) {//视频不支持缩放
                                continue
                            }
                            isPinching = true
                            isDragging = false
                            val zoom = event.calculateZoom()
                            val centroid = event.calculateCentroid(useCurrent = true)
                            val translation = lastCentroid?.let { centroid - it } ?: Offset.Zero
                            vm.onImagePinch(zoom, centroid, translation)
                            lastCentroid = centroid
                            event.changes.forEach { it.consume() }
                            continue
                        }
                        lastCentroid = null
                        if (isPinching || vm.videoIsSpeedUp) {//缩放和倍速播放期间不拖动
                            continue
                        }
                        val change = pressed[0]
                        velocityTracker.addPosition(change.uptimeMillis, change.position)
                        if (!isDragging) {
                            if ((change.position - start).getDistance() >= threshold) {
                                isDragging = true
                                vm.onImageDrag(change.position - start)
                            }
                        } else {
                            vm.onImageDrag(change.position - lastPosition)
                        }

                        //短距离快速滑动也会触发单击,移动一小段距离就取消单击
                        if (isDragging && (change.position - start).getDistance() > tapMaxMove) {
                            change.consume()
                        }
                        lastPosition = change.position
                    }
                    if (isPinching) {
                        vm.onImagePinchEnd(lastCentroid ?: lastPosition)
                    } else if (isDragging) {
                        val velocity = velocityTracker.calculateVelocity()
                        vm.onImageDragEnd(Velocity(velocity.x, velocity.y))
                    }
                }
            },
    )
}
