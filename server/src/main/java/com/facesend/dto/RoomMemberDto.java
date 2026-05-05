package com.facesend.dto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class RoomMemberDto {
    private String deviceId;
    private String nickname;
    private String joinedAt;

    public RoomMemberDto() {}

    public RoomMemberDto(String deviceId, String nickname, LocalDateTime joinedAt) {
        this.deviceId = deviceId;
        this.nickname = nickname;
        this.joinedAt = joinedAt != null ? joinedAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null;
    }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getJoinedAt() { return joinedAt; }
    public void setJoinedAt(String joinedAt) { this.joinedAt = joinedAt; }
}
