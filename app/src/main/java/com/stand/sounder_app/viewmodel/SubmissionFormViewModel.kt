package com.stand.sounder_app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stand.sounder_app.MyApp
import com.stand.sounder_app.data.model.CreateSubmissionRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** 平台选项（参考 docs/02-module-architecture.md §5） */
val SUBMISSION_PLATFORMS = listOf("哔哩", "抖音", "快手", "粉丝群")
/** 图片来源选项 */
val SUBMISSION_IMAGE_SOURCES = listOf("游戏截图", "动漫截图", "互联网", "视频截图")
/** 声音来源选项 */
val SUBMISSION_VOICE_SOURCES = listOf("游戏录制", "视频链接", "互联网")

private const val RESOURCE_NAME_MAX = 10
private const val APP_NAME_MAX = 10
private const val BRIEF_MAX = 50
private const val NICKNAME_MAX = 50

data class SubmissionFormState(
    val resourceName: String = "",
    val appName: String = "",
    val brief: String = "",
    val platform: String = "",
    val platformId: String = "",
    val nickname: String = "",
    val imageSource: String = "",
    val imageSourceLink: String = "",
    val voiceSource: String = "",
    val voiceSourceLink: String = "",
    val fileLink: String = "",
    val fieldErrors: Map<String, String> = emptyMap(),
    val isSubmitting: Boolean = false,
    val submitError: String? = null,
    val submitSuccess: Boolean = false
)

class SubmissionFormViewModel : ViewModel() {

    private val repository = MyApp.instance.submissionRepository

    private val _uiState = MutableStateFlow(SubmissionFormState())
    val uiState: StateFlow<SubmissionFormState> = _uiState.asStateFlow()

    fun update(field: (SubmissionFormState) -> SubmissionFormState) {
        _uiState.value = field(_uiState.value).copy(fieldErrors = emptyMap(), submitError = null)
    }

    /** 校验表单，返回字段错误映射；当备注/文件链接为空时，图片/声音来源链接为必填 */
    private fun validate(state: SubmissionFormState): Map<String, String> {
        val errors = mutableMapOf<String, String>()

        fun checkRequired(value: String, key: String, label: String, max: Int) {
            when {
                value.isBlank() -> errors[key] = "请填写$label"
                value.length > max -> errors[key] = label + "不能超过" + max + "个字符"
            }
        }

        checkRequired(state.resourceName, "resourceName", "资源包全称", RESOURCE_NAME_MAX)
        checkRequired(state.appName, "appName", "APP简称", APP_NAME_MAX)
        checkRequired(state.brief, "brief", "简述", BRIEF_MAX)
        checkRequired(state.nickname, "nickname", "昵称", NICKNAME_MAX)
        if (state.platform.isBlank()) errors["platform"] = "请选择平台"
        if (state.imageSource.isBlank()) errors["imageSource"] = "请选择图片来源"
        if (state.voiceSource.isBlank()) errors["voiceSource"] = "请选择声音来源"

        // 条件必填：fileLink 为空时，图片来源链接与声音来源链接必须填写
        if (state.fileLink.isBlank()) {
            if (state.imageSourceLink.isBlank()) errors["imageSourceLink"] = "未填备注时需填写图片来源链接"
            if (state.voiceSourceLink.isBlank()) errors["voiceSourceLink"] = "未填备注时需填写声音来源链接"
        }

        return errors
    }

    fun submit(onSuccess: () -> Unit) {
        val state = _uiState.value
        val errors = validate(state)
        if (errors.isNotEmpty()) {
            _uiState.value = state.copy(fieldErrors = errors)
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isSubmitting = true, submitError = null)
            val request = CreateSubmissionRequest(
                resourceName = state.resourceName.trim(),
                appName = state.appName.trim(),
                brief = state.brief.trim(),
                platform = state.platform,
                platformId = state.platformId.ifBlank { null },
                nickname = state.nickname.trim(),
                imageSource = state.imageSource,
                imageSourceLink = state.imageSourceLink.ifBlank { null },
                voiceSource = state.voiceSource,
                voiceSourceLink = state.voiceSourceLink.ifBlank { null },
                fileLink = state.fileLink.ifBlank { null }
            )
            repository.createSubmission(request).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(isSubmitting = false, submitSuccess = true)
                    onSuccess()
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        submitError = e.message ?: "提交失败"
                    )
                }
            )
        }
    }
}
