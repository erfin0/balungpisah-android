package com.balungpisah.data.api

import com.balungpisah.data.models.*
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface AttachmentApiService {
    
    @Multipart
    @POST("/api/citizen-report-agent/threads/{thread_id}/attachments")
    suspend fun uploadAttachment(
        @Path("thread_id") threadId: String,
        @Part file: MultipartBody.Part
    ): Response<ApiResponse<ThreadAttachment>>
    
    @GET("/api/citizen-report-agent/threads/{thread_id}/attachments")
    suspend fun getAttachments(
        @Path("thread_id") threadId: String
    ): Response<ApiResponse<List<ThreadAttachment>>>
    
    @GET("/api/citizen-report-agent/threads/{thread_id}/attachments/count")
    suspend fun getAttachmentCount(
        @Path("thread_id") threadId: String
    ): Response<ApiResponse<AttachmentCountResponse>>
    
    @DELETE("/api/citizen-report-agent/threads/{thread_id}/attachments/{attachment_id}")
    suspend fun deleteAttachment(
        @Path("thread_id") threadId: String,
        @Path("attachment_id") attachmentId: String
    ): Response<ApiResponse<Map<String, Boolean>>>
    
    @GET("/api/citizen-report-agent/rate-limit")
    suspend fun getRateLimit(): Response<ApiResponse<UserRateLimitStatus>>
    
    @GET("/api/citizen-report-agent/threads")
    suspend fun getThreads(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20
    ): Response<ApiResponse<List<ThreadResponse>>>
    
    @GET("/api/citizen-report-agent/threads/{id}")
    suspend fun getThreadDetail(
        @Path("id") threadId: String
    ): Response<ApiResponse<ThreadDetailResponse>>
    
    @GET("/api/citizen-report-agent/threads/{id}/messages")
    suspend fun getMessages(
        @Path("id") threadId: String,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 50
    ): Response<ApiResponse<List<MessageResponse>>>
}