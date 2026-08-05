package com.stand.sounder_app.data.model

import com.google.gson.annotations.SerializedName

/**
 * 商店资源列表响应（对齐真实后端 /api/sounders）
 * 外层: {"message":"操作成功","data":{"total":131,"page":1,"size":10,"data":[...]}}
 * 成功判断: data != null
 *
 * 不使用泛型响应包装类：R8 全模式会剥离类型变量 T 的签名，导致 Gson 反序列化时
 * "java.lang.Class cannot be cast to java.lang.reflect.ParameterizedType"。
 * 这里全部拍平为具体类型（无类型变量），List<RemoteResource> 这种具体参数化类型安全。
 */
class RemoteResourceListResponse(
    val message: String = "",
    val data: RemoteResourceListData? = null
) {
    val isSuccess: Boolean get() = data != null
}

/** 列表分页数据体（列表 JSON 键为 "data"，用 SerializedName 映射为 items） */
class RemoteResourceListData(
    val size: Int = 10,
    @SerializedName("data")
    val items: List<RemoteResource> = emptyList()
)

/** 商店资源详情响应：{"message":"操作成功","data":{...}} */
class RemoteResourceDetailResponse(
    val message: String = "",
    val data: RemoteResource? = null
) {
    val isSuccess: Boolean get() = data != null
}

/** 投稿列表响应（列表键为 "submissions"） */
data class SubmissionListResponse(
    val message: String = "",
    val data: SubmissionListData? = null
) {
    val isSuccess: Boolean get() = data != null
}

data class SubmissionListData(
    val submissions: List<Submission> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val size: Int = 10,
    val statusCounts: SubmissionStatusCounts? = null
)

data class SubmissionStatusCounts(
    val pending: Int = 0,
    @SerializedName("in_progress")
    val inProgress: Int = 0,
    val completed: Int = 0
)

/**
 * 远程资源（来自商店 API /api/sounders）—— 字段映射对齐真实后端
 */
data class RemoteResource(
    val id: String = "",
    val name: String = "",
    val displayName: String = "",
    val description: String = "",
    val icon: String = "",
    val audioList: List<RemoteAudioItem> = emptyList(),
    val size: String = "0",   // API 返回格式化字符串如 "24.40KB"
    val publishDate: String = "",
    val resourceId: String = ""
)

data class RemoteAudioItem(
    val id: String = "",
    val name: String = "",
    @SerializedName("url")
    val url: String = "",
    val duration: Long = 0L   // 毫秒
)

// ===== 投稿模型（对齐真实后端 /api/submissions） =====

enum class SubmissionStatus(val apiValue: String, val displayName: String, val sideColor: Long) {
    PENDING("pending", "待排表", 0xFFF59E0B),
    IN_PROGRESS("in_progress", "进行中", 0xFF5DA3E8),
    COMPLETED("completed", "已完成", 0xFF10B981);

    companion object {
        fun fromApi(value: String?): SubmissionStatus = when (value) {
            "pending" -> PENDING
            "in_progress" -> IN_PROGRESS
            "completed" -> COMPLETED
            else -> PENDING
        }
    }
}

data class Submission(
    val id: Int = 0,
    val resourceName: String = "",
    val appName: String = "",
    val brief: String = "",
    val platform: String = "",
    val platformId: String? = null,
    val nickname: String = "",
    val imageSource: String = "",
    val imageSourceLink: String? = null,
    val voiceSource: String = "",
    val voiceSourceLink: String? = null,
    val fileLink: String? = null,
    val status: String = "pending",
    val plannedDate: String? = null,
    val reviewStatus: String? = null,
    val createdAt: String? = null
) {
    val statusEnum: SubmissionStatus get() = SubmissionStatus.fromApi(status)
}

