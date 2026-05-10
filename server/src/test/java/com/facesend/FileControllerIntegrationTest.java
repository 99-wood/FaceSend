package com.facesend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.facesend.dto.CreateRoomRequest;
import com.facesend.entity.Room;
import com.facesend.repository.RoomRepository;
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
public class FileControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RoomRepository roomRepository;

    @Test
    public void testGetFileList_EmptyRoom() throws Exception {
        Room room = new Room();
        room.setCode("111222");
        room.setCreatorId("test-creator");
        room.setExpireAt(java.time.LocalDateTime.now().plusMinutes(30));
        Room savedRoom = roomRepository.save(room);

        mockMvc.perform(get("/api/file/list/" + savedRoom.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.data", is(empty())));
    }
}
