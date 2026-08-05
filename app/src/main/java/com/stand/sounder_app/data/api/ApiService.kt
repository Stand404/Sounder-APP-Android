package com.stand.sounder_app.data.api

import com.stand.sounder_app.data.model.RemoteResourceDetailResponse
import com.stand.sounder_app.data.model.RemoteResourceListResponse
import com.stand.sounder_app.data.model.SubmissionListResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    // ===== 资源（真实后端 /api/sounders） =====

    @GET("api/sounders")
    suspend fun getResourceList(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 10
    ): RemoteResourceListResponse

    @GET("api/sounders/{id}")
    suspend fun getResourceDetail(
        @Path("id") id: String
    ): RemoteResourceDetailResponse

    @GET("api/sounders")
    suspend fun searchResources(
        @Query("keyword") keyword: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 10
    ): RemoteResourceListResponse

    // ===== 投稿（真实后端 /api/submissions） =====

    @GET("api/submissions")
    suspend fun getSubmissions(
        @Query("status") status: String? = null,
        @Query("keyword") keyword: String? = null,
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 10
    ): SubmissionListResponse
}
