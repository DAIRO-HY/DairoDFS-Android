package cn.dairo.dfs.api.model

import kotlinx.serialization.Serializable

@Serializable
data class GlobalModel(

    /** 集群ID,同一个集群的服务器共用缓存 */
    val clusterId: String,
)
