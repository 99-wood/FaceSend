package com.facesend.controller;

import com.facesend.dto.ApiResponse;
import com.facesend.dto.CreateRoomRequest;
import com.facesend.dto.JoinRoomRequest;
import com.facesend.entity.Room;
import com.facesend.service.RoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/room")
@CrossOrigin(origins = "*")
public class RoomController {

    @Autowired
    private RoomService roomService;

    @PostMapping(value = "/create", consumes = {"application/json", "text/plain"})
    public ApiResponse<Map<String, Object>> createRoom(@RequestBody CreateRoomRequest request) {
        Room room = roomService.createRoom(request);
        return ApiResponse.success(Map.of(
                "roomId", room.getId(),
                "code", room.getCode()
        ));
    }

    @PostMapping(value = "/join", consumes = {"application/json", "text/plain"})
    public ApiResponse<Map<String, Object>> joinRoom(@RequestBody JoinRoomRequest request) {
        Optional<Room> roomOpt = roomService.joinRoom(request);
        if (roomOpt.isEmpty()) {
            return ApiResponse.error("房间不存在、已关闭或距离过远");
        }
        Room room = roomOpt.get();
        return ApiResponse.success(Map.of(
                "roomId", room.getId(),
                "code", room.getCode()
        ));
    }

    @PostMapping("/close/{roomId}")
    public ApiResponse<Void> closeRoom(@PathVariable Long roomId) {
        boolean success = roomService.closeRoom(roomId);
        if (success) {
            return ApiResponse.success();
        }
        return ApiResponse.error("关闭房间失败");
    }
}
