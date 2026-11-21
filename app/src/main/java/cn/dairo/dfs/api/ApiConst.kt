package cn.dairo.dfs.api

/**
 * Api地址
 */
object ApiConst {

    const val COMMON_GLOBAL = "/api/common/global"

    const val LOGIN_DO_LOGIN = "/api/login/do_login"
    const val LOGIN_LOGOUT = "/api/login/logout"

    const val MINE_INFO = "/api/mine/info"
    const val MINE_NICKNAME = "/api/mine/nickname"
    const val MINE_AVATAR = "/api/mine/avatar"
    const val AVATAR = "/api/avatar"
    const val MODIFY_PWD = "/api/modify_pwd"

    const val FILES_LIST = "/api/files/list"
    const val FILES_ALBUM = "/api/files/album"
    const val FILES_CREATE_FOLDER = "/api/files/create_folder"
    const val FILES_DELETE = "/api/files/delete"
    const val FILES_RENAME = "/api/files/rename"
    const val FILES_CAPTURE_TIME = "/api/files/capture_time"
    const val FILES_COPY = "/api/files/copy"
    const val FILES_MOVE = "/api/files/move"
    const val FILES_PROPERTY = "/api/files/property"

    const val TRASH_LIST = "/api/trash/list"
    const val TRASH_DELETE = "/api/trash/delete"
    const val TRASH_RECOVER = "/api/trash/recover"

    const val FILE_UPLOAD_EXISTS = "/api/file_upload/exists"
}
