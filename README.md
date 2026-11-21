# DairoDFS-Android

[DairoDFS](https://github.com/DAIRO-HY/DairoDFS) 的 Android 客户端，Jetpack Compose 编写。功能、界面和代码结构与 [DairoDFS-IOS](https://github.com/DAIRO-HY/DairoDFS-IOS) 保持一致，类名、包名、文件名一一对应，方便两边同步维护。

## 功能

- 文件：浏览、上传文件和文件夹、上传相册里的照片和视频、创建文件夹、复制、剪切、粘贴、重命名、删除、下载
- 相册：按全部、按年、按月浏览，相册同步（检查哪些照片已经上传、删除已上传的照片）
- 查看：图片缩放、左右切换、实况照片、视频播放（自动、原画、流畅）、属性和修改拍摄时间、下载、保存到系统相册
- 音频播放：支持后台播放和通知栏控制
- 回收站、上传管理、下载管理（下载和缓存）、多账号切换、个人资料、修改密码

## 运行环境

- Android 8.0（API 26）及以上，支持手机和平板（arm64-v8a、armeabi-v7a、x86_64）
- 编译：JDK 25（Gradle 会通过 foojay 自动下载）、Android SDK 37

## 依赖

| 依赖 | 说明 | 引用方式 |
|---|---|---|
| [DairoUI_Android](https://github.com/DAIRO-HY/DairoUI_Android) | 设置页风格的 UI 组件、Toast、Loading、主题 | 通过 jitpack 引用 GitHub 上发布的版本 |
| [DairoDFS](https://github.com/DAIRO-HY/DairoDFS) 的 Android SDK | 文件下载、缓存、上传 | `app/libs/dairo-file-sdk-*.aar` |

更新 SDK：在 DairoDFS 里执行 `./dairo-file-sdk/build.sh android`，把 `dairo-file-sdk/dist/android/` 下的 AAR 复制到 `app/libs/`（文件名变了时同时修改 `app/build.gradle.kts`）。

## 签名

签名信息不在代码里，按下面的顺序读取，都没有时 debug 使用默认的调试签名，release 生成未签名的 APK：

1. 环境变量 `DAIRO_KEYSTORE_FILE`、`DAIRO_KEYSTORE_PASSWORD`、`DAIRO_KEY_ALIAS`、`DAIRO_KEY_PASSWORD`（编译服务器用）
2. 项目根目录的 `keystore.properties`（本机用，格式见 `keystore.properties.example`，不提交到 git）

## 和 iOS 版的差异

- 没有毛玻璃效果，浮动的按钮、Tab 栏用不透明背景加阴影
- 查看页的模糊背景需要 Android 12 及以上，更早的系统用黑色背景
- 上传、下载时用前台服务在通知栏显示进度，App 进入后台后继续传输（`util/FileBackgroundTransfer.kt`）
- 音频的后台播放、通知栏和锁屏控制使用 Media3 的 `MediaSessionService`（`page/audioplayer/AudioPlaybackService.kt`）
- 上传文件、文件夹使用系统的文件选择器，选择时申请永久访问权限，App 重启后未完成的上传可以继续
- Android 没有实况照片：上传相册时按原文件上传；服务器上的实况照片可以查看和播放，保存到相册时只保存照片部分
- 删除已上传的照片：Android 11 及以上由系统弹框确认，Android 10 及以下需要存储权限
- 保存到相册：Android 10 及以上不需要权限，Android 9 及以下需要存储权限

## 开源协议

[Apache License 2.0](./LICENSE)
