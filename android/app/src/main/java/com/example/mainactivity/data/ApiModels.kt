package com.example.mainactivity.data

data class ApiResponse<T>(
    val code: Int,
    val message: String,
    val data: T?
)

data class CreateRoomRequest(
    val creatorId: String,
    val latitude: Double,
    val longitude: Double
)

data class JoinRoomRequest(
    val code: String,
    val userId: String,
    val latitude: Double,
    val longitude: Double
)

data class RoomResponse(
    val roomId: Long,
    val code: String
)

data class FileInfo(
    val id: Long,
    val roomId: Long,
    val uploaderId: String,
    val fileName: String,
    val fileSize: Long,
    val mimeType: String?
)
