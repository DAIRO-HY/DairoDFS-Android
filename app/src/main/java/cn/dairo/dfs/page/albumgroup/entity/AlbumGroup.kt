package cn.dairo.dfs.page.albumgroup.entity

import cn.dairo.dfs.api.model.FileModel
import java.util.Calendar

/**
 * 相册的显示方式
 */
enum class AlbumViewMode {

    /** 按年分组 */
    YEAR,

    /** 按月分组 */
    MONTH,

    /** 显示全部照片 */
    ALL,
}

/**
 * 分组里的一张照片
 */
class AlbumGroupPhoto(
    val fm: FileModel,

    /** 照片左上角显示的日期,按月分组时是:4日,按年分组时是:11月4日 */
    val dateLabel: String,
)

/**
 * 一个分组,对应一年或者一个月
 */
class AlbumGroup(

    /** 分组标题,比如:2026年、2026年11月 */
    val title: String,

    /** 这个分组要显示的照片,最多4张 */
    val photos: List<AlbumGroupPhoto>,
)

/**
 * 把相册的照片整理成分组
 */
object AlbumGroupBuilder {

    /** 每组最多显示的照片数 */
    private const val MAX_PHOTO_COUNT = 4

    /**
     * 按年分组
     * @param fileModels 相册的全部照片,要求已经按拍摄时间排好序
     */
    fun buildYearGroups(fileModels: List<FileModel>): List<AlbumGroup> {
        return this.build(fileModels, { "${it.year}年" }, { "${it.month}月${it.day}日" })
    }

    /**
     * 按月分组
     * @param fileModels 相册的全部照片,要求已经按拍摄时间排好序
     */
    fun buildMonthGroups(fileModels: List<FileModel>): List<AlbumGroup> {
        return this.build(fileModels, { "${it.year}年${it.month}月" }, { "${it.day}日" })
    }

    /** 拍摄日期 */
    private data class DateComponents(val year: Int, val month: Int, val day: Int)

    /**
     * 整理分组
     * @param fileModels 相册的全部照片,要求已经按拍摄时间排好序
     * @param getTitle 根据拍摄日期得到分组标题,标题相同的照片属于同一组
     * @param getDateLabel 根据拍摄日期得到照片左上角显示的日期
     */
    private fun build(fileModels: List<FileModel>, getTitle: (DateComponents) -> String, getDateLabel: (DateComponents) -> String): List<AlbumGroup> {
        val groups = ArrayList<AlbumGroup>()
        val calendar = Calendar.getInstance()

        //当前分组的标题
        var currentTitle = ""

        //当前分组里每天的第一张照片
        val firstPhotos = ArrayList<AlbumGroupPhoto>()

        //上一张照片的拍摄日期,用来判断是不是新的一天
        var lastDate: DateComponents? = null
        for (fm in fileModels) {
            calendar.timeInMillis = fm.date
            val date = DateComponents(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH))
            val title = getTitle(date)
            if (title != currentTitle) {//进入了新的分组,把上一个分组保存起来
                if (firstPhotos.isNotEmpty()) {
                    groups.add(AlbumGroup(currentTitle, this.pickPhotos(firstPhotos)))
                }
                currentTitle = title
                firstPhotos.clear()
                lastDate = null
            }
            if (date != lastDate) {//这一天的第一张照片
                firstPhotos.add(AlbumGroupPhoto(fm, getDateLabel(date)))
                lastDate = date
            }
        }
        if (firstPhotos.isNotEmpty()) {//保存最后一个分组
            groups.add(AlbumGroup(currentTitle, this.pickPhotos(firstPhotos)))
        }
        return groups
    }

    /**
     * 从每天的第一张照片里,按相同的间隔取出要显示的照片
     * 比如有30天有照片,间隔是30/4=7,取第1、8、15、22天。有10天有照片,间隔是10/4=2,取第1、3、5、7天
     * @param firstPhotos 每天的第一张照片
     */
    private fun pickPhotos(firstPhotos: List<AlbumGroupPhoto>): List<AlbumGroupPhoto> {
        if (firstPhotos.size <= MAX_PHOTO_COUNT) {//不够4天就全部显示
            return firstPhotos.toList()
        }
        val step = firstPhotos.size / MAX_PHOTO_COUNT
        return (0 until MAX_PHOTO_COUNT).map { firstPhotos[it * step] }
    }
}
