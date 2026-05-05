package com.facesend.controller;

import com.facesend.dto.ApiResponse;
import com.facesend.dto.CreateRoomRequest;
import com.facesend.dto.JoinRoomRequest;
import com.facesend.dto.RoomMembersResponseDto;
import com.facesend.dto.UpdateNicknameRequest;
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
                "code", room.getCode(),
                "allowMemberUpload", room.getAllowMemberUpload()
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
                "code", room.getCode(),
                "allowMemberUpload", room.getAllowMemberUpload()
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

    @GetMapping("/members/{roomId}")
    public ApiResponse<RoomMembersResponseDto> getRoomMembers(
            @PathVariable Long roomId,
            @RequestParam(required = false) String deviceId) {
        RoomMembersResponseDto response = roomService.getRoomMembers(roomId, deviceId);
        return ApiResponse.success(response);
    }

    @PostMapping("/allow-upload/{roomId}")
    public ApiResponse<Void> setAllowMemberUpload(
            @PathVariable Long roomId,
            @RequestParam String deviceId,
            @RequestParam boolean allow) {
        boolean success = roomService.setAllowMemberUpload(roomId, deviceId, allow);
        if (success) {
            return ApiResponse.success();
        }
        return ApiResponse.error("操作失败，仅房主可修改");
    }

    @PostMapping(value = "/member/nickname", consumes = {"application/json", "text/plain"})
    public ApiResponse<Void> updateNickname(@RequestBody UpdateNicknameRequest request) {
        boolean success = roomService.updateNickname(request);
        if (success) {
            return ApiResponse.success();
        }
        return ApiResponse.error("更新昵称失败");
    }
}
