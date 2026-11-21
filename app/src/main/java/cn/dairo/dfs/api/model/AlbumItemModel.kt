package cn.dairo.dfs.api.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long

/**
 * 相册里的一条数据,服务端为了减小数据量,以数组的形式返回:[文件ID, 拍摄时间(秒), 扩展名, 是否有缩略图, 视频时长(毫秒,图片为0)]
 */
@Serializable(with = AlbumItemModelSerializer::class)
data class AlbumItemModel(
    val id: Long,
    val time: Long,
    val ext: String,
    val hasThumb: Boolean,
    val duration: Long,
) {

    /** 转换成文件信息,相册接口没有返回文件名和大小,文件名用文件ID代替 */
    val fileModel: FileModel
        get() = FileModel(id = this.id, name = "${this.id}.${this.ext}", size = 0, fileFlag = true, date = this.time * 1000, hasThumb = this.hasThumb, duration = this.duration)
}

/**
 * 按数组读写相册数据
 */
object AlbumItemModelSerializer : KSerializer<AlbumItemModel> {

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("AlbumItemModel")

    override fun deserialize(decoder: Decoder): AlbumItemModel {
        val array = (decoder as JsonDecoder).decodeJsonElement() as JsonArray
        return AlbumItemModel(
            id = array[0].jsonPrimitive.long,
            time = array[1].jsonPrimitive.long,
            ext = array[2].jsonPrimitive.content,
            hasThumb = array[3].jsonPrimitive.boolean,
            duration = array[4].jsonPrimitive.long,
        )
    }

    override fun serialize(encoder: Encoder, value: AlbumItemModel) {
        val array = JsonArray(listOf(JsonPrimitive(value.id), JsonPrimitive(value.time), JsonPrimitive(value.ext), JsonPrimitive(value.hasThumb), JsonPrimitive(value.duration)))
        (encoder as JsonEncoder).encodeJsonElement(array)
    }
}
