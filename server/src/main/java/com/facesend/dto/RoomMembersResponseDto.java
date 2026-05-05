package com.facesend.dto;

import java.util.List;

public class RoomMembersResponseDto {
    private List<RoomMemberDto> members;
    private boolean allowMemberUpload;

    public RoomMembersResponseDto(List<RoomMemberDto> members, boolean allowMemberUpload) {
        this.members = members;
        this.allowMemberUpload = allowMemberUpload;
    }

    public List<RoomMemberDto> getMembers() { return members; }
    public void setMembers(List<RoomMemberDto> members) { this.members = members; }
    public boolean isAllowMemberUpload() { return allowMemberUpload; }
    public void setAllowMemberUpload(boolean allowMemberUpload) { this.allowMemberUpload = allowMemberUpload; }
}
