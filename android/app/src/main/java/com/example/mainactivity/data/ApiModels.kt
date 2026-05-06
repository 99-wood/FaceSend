package com.example.mainactivity.data

data class ApiResponse<T>(
    val code: Int,
    val message: String,
    val data: T?
)

data class CreateRoomRequest(
    val creatorId: String,
    val latitude: Double,
    val longitude: Double,
    val nickname: String
)

data class JoinRoomRequest(
    val code: String,
    val userId: String,
    val latitude: Double,
    val longitude: Double,
    val nickname: String
)

data class RoomResponse(
    val roomId: Long,
    val code: String,
    val allowMemberUpload: Boolean = false
)

data class FileInfo(
    val id: Long,
    val roomId: Long,
    val uploaderId: String,
    val fileName: String,
    val fileSize: Long,
    val mimeType: String?
)

data class UpdateNicknameRequest(
    val roomId: Long,
    val deviceId: String,
    val nickname: String
)

data class RoomMemberInfo(
    val deviceId: String,
    val nickname: String,
    val joinedAt: String?
)

data class RoomMembersResponse(
    val members: List<RoomMemberInfo>,
    val allowMemberUpload: Boolean
)
