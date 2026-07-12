package com.stand.sounder_app.data.repository

import com.stand.sounder_app.data.api.ApiService
import com.stand.sounder_app.data.model.CreateSubmissionRequest
import com.stand.sounder_app.data.model.Submission

class SubmissionRepository(
    private val apiService: ApiService
) {

    /** 获取投稿列表（按状态/关键字筛选，分页） */
    suspend fun getSubmissions(
        status: String? = null,
        keyword: String? = null,
        page: Int = 1,
        size: Int = 20
    ): Result<List<Submission>> {
        return try {
            val response = apiService.getSubmissions(status, keyword, page, size)
            if (response.isSuccess) {
                Result.success(response.data?.submissions ?: emptyList())
            } else {
                Result.failure(Exception(response.message.ifEmpty { "加载失败" }))
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Result.failure(e)
        }
    }

    /** 新建投稿 */
    suspend fun createSubmission(request: CreateSubmissionRequest): Result<Submission> {
        return try {
            val response = apiService.createSubmission(request)
            if (response.isSuccess && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message.ifEmpty { "提交失败" }))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
