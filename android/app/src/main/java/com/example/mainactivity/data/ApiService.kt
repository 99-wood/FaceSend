package com.example.mainactivity.data

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    @POST("api/room/create")
    suspend fun createRoom(@Body request: CreateRoomRequest): ApiResponse<RoomResponse>

    @POST("api/room/join")
    suspend fun joinRoom(@Body request: JoinRoomRequest): ApiResponse<RoomResponse>

    @GET("api/file/list/{roomId}")
    suspend fun listFiles(@Path("roomId") roomId: Long): ApiResponse<List<FileInfo>>

    @Multipart
    @POST("api/file/upload")
    suspend fun uploadFile(
        @Part("roomId") roomId: RequestBody,
        @Part("uploaderId") uploaderId: RequestBody,
        @Part file: MultipartBody.Part
    ): ApiResponse<Map<String, Any>>

    @Streaming
    @GET("api/file/download/{id}")
    suspend fun downloadFile(@Path("id") id: Long): Response<ResponseBody>

    @POST("api/room/close/{roomId}")
    suspend fun closeRoom(@Path("roomId") roomId: Long): ApiResponse<Void>

    @GET("api/room/members/{roomId}")
    suspend fun getRoomMembers(
        @Path("roomId") roomId: Long,
        @Query("deviceId") deviceId: String
    ): ApiResponse<RoomMembersResponse>

    @POST("api/room/allow-upload/{roomId}")
    suspend fun setAllowMemberUpload(
        @Path("roomId") roomId: Long,
        @Query("deviceId") deviceId: String,
        @Query("allow") allow: Boolean
    ): ApiResponse<Void>

    @POST("api/room/member/nickname")
    suspend fun updateNickname(@Body request: UpdateNicknameRequest): ApiResponse<Void>
}
