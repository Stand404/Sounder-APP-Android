package com.stand.sounder_app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stand.sounder_app.MyApp
import com.stand.sounder_app.R
import com.stand.sounder_app.data.model.Submission
import com.stand.sounder_app.data.model.SubmissionStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SubmissionDateGroup(
    val date: String = "",
    val items: List<Submission> = emptyList(),
    val timelineColor: Long = SubmissionStatus.IN_PROGRESS.sideColor
)

data class SubmissionsUiState(
    val submissions: List<Submission> = emptyList(),
    val groupedSubmissions: List<SubmissionDateGroup> = emptyList(),
    val selectedStatus: SubmissionStatus = SubmissionStatus.IN_PROGRESS,
    val isTimelineView: Boolean = true,
    val keyword: String = "",
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = true,
    val error: String? = null
)

class SubmissionsViewModel : ViewModel() {

    private val repository = MyApp.instance.submissionRepository

    private val _uiState = MutableStateFlow(SubmissionsUiState())
    val uiState: StateFlow<SubmissionsUiState> = _uiState.asStateFlow()

    private var currentPage = 1
    private val pageSize = 20
    private var loadJob: Job? = null

    init {
        loadSubmissions()
    }

    fun loadSubmissions(reset: Boolean = true) {
        // 切换筛选/关键字时，先取消上一个仍在进行的请求，避免竞态与结果覆盖
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            if (reset) {
                currentPage = 1
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            } else {
                _uiState.value = _uiState.value.copy(isLoadingMore = true)
            }

            val state = _uiState.value
            val result = repository.getSubmissions(
                status = state.selectedStatus.apiValue,
                keyword = state.keyword.ifBlank { null },
                page = currentPage,
                size = pageSize
            )

            result.fold(
                onSuccess = { list ->
                    val merged = if (reset) list else _uiState.value.submissions + list
                    val isTimeline = state.selectedStatus != SubmissionStatus.PENDING
                    val groups = if (isTimeline) buildGroups(merged, state.selectedStatus.sideColor) else emptyList()
                    _uiState.value = _uiState.value.copy(
                        submissions = merged,
                        groupedSubmissions = groups,
                        isTimelineView = isTimeline,
                        isLoading = false,
                        isLoadingMore = false,
                        hasMore = list.size >= pageSize
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        error = error.message ?: MyApp.instance.getString(R.string.load_failed)
                    )
                }
            )
        }
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoadingMore || !state.hasMore || state.isLoading) return
        currentPage++
        loadSubmissions(reset = false)
    }

    fun setStatusFilter(status: SubmissionStatus) {
        if (_uiState.value.selectedStatus == status) return
        val isTimeline = status != SubmissionStatus.PENDING
        _uiState.value = _uiState.value.copy(
            selectedStatus = status,
            isTimelineView = isTimeline,
            groupedSubmissions = if (isTimeline) _uiState.value.groupedSubmissions else emptyList()
        )
        loadSubmissions()
    }

    fun setKeyword(keyword: String) {
        if (_uiState.value.keyword == keyword) return
        _uiState.value = _uiState.value.copy(keyword = keyword)
        loadSubmissions()
    }

    /** 按计划日期分组（空日期归为「未排期」），按日期降序 */
    private fun buildGroups(
        list: List<Submission>,
        timelineColor: Long
    ): List<SubmissionDateGroup> {
        return list.groupBy { it.plannedDate?.takeIf { d -> d.isNotBlank() } ?: MyApp.instance.getString(R.string.submission_no_schedule) }
            .toSortedMap(compareByDescending { it })
            .map { (date, items) -> SubmissionDateGroup(date = date, items = items, timelineColor = timelineColor) }
    }

}


