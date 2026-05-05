package com.facesend.dto;

public class JoinRoomRequest {
    private String code;
    private String userId;
    private Double latitude;
    private Double longitude;
    private String nickname;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
}
