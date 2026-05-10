package com.facesend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.facesend.dto.CreateRoomRequest;
import com.facesend.dto.JoinRoomRequest;
import com.facesend.dto.UpdateNicknameRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class RoomControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String createRoom(String creatorId, String nickname) throws Exception {
        CreateRoomRequest req = new CreateRoomRequest();
        req.setCreatorId(creatorId);
        req.setLatitude(39.9);
        req.setLongitude(116.4);
        req.setNickname(nickname);

        String result = mockMvc.perform(post("/api/room/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)))
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(result).get("data").get("code").asText();
    }

    @Test
    public void testCreateRoom() throws Exception {
        CreateRoomRequest request = new CreateRoomRequest();
        request.setCreatorId("test-device-001");
        request.setLatitude(39.9042);
        request.setLongitude(116.4074);
        request.setNickname("测试用户");

        mockMvc.perform(post("/api/room/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.data.code").isString())
                .andExpect(jsonPath("$.data.code", hasLength(6)))
                .andExpect(jsonPath("$.data.allowMemberUpload", is(false)));
    }

    @Test
    public void testCreateAndJoinRoom() throws Exception {
        String code = createRoom("creator-001", "房主");

        JoinRoomRequest joinReq = new JoinRoomRequest();
        joinReq.setCode(code);
        joinReq.setUserId("joiner-001");
        joinReq.setLatitude(39.9);
        joinReq.setLongitude(116.4);
        joinReq.setNickname("加入者");

        mockMvc.perform(post("/api/room/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(joinReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.data.code", is(code)));
    }

    @Test
    public void testJoinRoom_InvalidCode() throws Exception {
        JoinRoomRequest joinReq = new JoinRoomRequest();
        joinReq.setCode("999999");
        joinReq.setUserId("user-x");
        joinReq.setLatitude(39.9);
        joinReq.setLongitude(116.4);
        joinReq.setNickname("用户X");

        mockMvc.perform(post("/api/room/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(joinReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(not(200))));
    }

    @Test
    public void testGetRoomMembers() throws Exception {
        String createResult = mockMvc.perform(post("/api/room/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReqWith("member-test-creator", "房主A"))))
                .andReturn().getResponse().getContentAsString();
        var node = objectMapper.readTree(createResult);
        long roomId = node.get("data").get("roomId").asLong();
        String roomCode = node.get("data").get("code").asText();

        JoinRoomRequest joinReq = new JoinRoomRequest();
        joinReq.setCode(roomCode);
        joinReq.setUserId("member-joiner-001");
        joinReq.setLatitude(39.9);
        joinReq.setLongitude(116.4);
        joinReq.setNickname("加入者B");

        mockMvc.perform(post("/api/room/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(joinReq)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/room/members/" + roomId)
                        .param("deviceId", "member-test-creator"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.data.members", hasSize(2)))
                .andExpect(jsonPath("$.data.allowMemberUpload", is(false)));
    }

    @Test
    public void testUpdateNickname() throws Exception {
        String createResult = mockMvc.perform(post("/api/room/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReqWith("nick-test-device", "原始昵称"))))
                .andReturn().getResponse().getContentAsString();
        long roomId = objectMapper.readTree(createResult).get("data").get("roomId").asLong();

        UpdateNicknameRequest updateReq = new UpdateNicknameRequest();
        updateReq.setRoomId(roomId);
        updateReq.setDeviceId("nick-test-device");
        updateReq.setNickname("新昵称");

        mockMvc.perform(post("/api/room/member/nickname")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)));

        mockMvc.perform(get("/api/room/members/" + roomId)
                        .param("deviceId", "nick-test-device"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.members[0].nickname", is("新昵称")));
    }

    @Test
    public void testAllowMemberUpload() throws Exception {
        String createResult = mockMvc.perform(post("/api/room/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReqWith("upload-owner", "房主"))))
                .andReturn().getResponse().getContentAsString();
        long roomId = objectMapper.readTree(createResult).get("data").get("roomId").asLong();

        mockMvc.perform(post("/api/room/allow-upload/" + roomId)
                        .param("deviceId", "upload-owner")
                        .param("allow", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)));

        mockMvc.perform(get("/api/room/members/" + roomId)
                        .param("deviceId", "upload-owner"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.allowMemberUpload", is(true)));
    }

    @Test
    public void testAllowMemberUpload_NonOwnerDenied() throws Exception {
        String createResult = mockMvc.perform(post("/api/room/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReqWith("real-owner", "房主"))))
                .andReturn().getResponse().getContentAsString();
        long roomId = objectMapper.readTree(createResult).get("data").get("roomId").asLong();

        mockMvc.perform(post("/api/room/allow-upload/" + roomId)
                        .param("deviceId", "not-the-owner")
                        .param("allow", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(not(200))));
    }

    @Test
    public void testCloseRoom() throws Exception {
        String createResult = mockMvc.perform(post("/api/room/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReqWith("close-test", "房主"))))
                .andReturn().getResponse().getContentAsString();
        long roomId = objectMapper.readTree(createResult).get("data").get("roomId").asLong();
        String code = objectMapper.readTree(createResult).get("data").get("code").asText();

        mockMvc.perform(post("/api/room/close/" + roomId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)));

        JoinRoomRequest joinReq = new JoinRoomRequest();
        joinReq.setCode(code);
        joinReq.setUserId("late-joiner");
        joinReq.setLatitude(39.9);
        joinReq.setLongitude(116.4);

        mockMvc.perform(post("/api/room/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(joinReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(not(200))));
    }

    private CreateRoomRequest createReqWith(String creatorId, String nickname) {
        CreateRoomRequest req = new CreateRoomRequest();
        req.setCreatorId(creatorId);
        req.setLatitude(39.9);
        req.setLongitude(116.4);
        req.setNickname(nickname);
        return req;
    }
}
