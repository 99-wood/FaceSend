package com.facesend;

import com.facesend.dto.CreateRoomRequest;
import com.facesend.dto.JoinRoomRequest;
import com.facesend.dto.RoomMembersResponseDto;
import com.facesend.dto.UpdateNicknameRequest;
import com.facesend.entity.Room;
import com.facesend.entity.RoomMember;
import com.facesend.repository.RoomMemberRepository;
import com.facesend.repository.RoomRepository;
import com.facesend.service.FileService;
import com.facesend.service.RoomService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RoomServiceUnitTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private RoomMemberRepository roomMemberRepository;

    @Mock
    private FileService fileService;

    @InjectMocks
    private RoomService roomService;

    @Test
    public void testCreateRoom_ShouldGenerate6DigitCode() {
        ReflectionTestUtils.setField(roomService, "roomExpireMinutes", 30);

        CreateRoomRequest request = new CreateRoomRequest();
        request.setCreatorId("test-user-123");
        request.setLatitude(39.9);
        request.setLongitude(116.4);
        request.setNickname("测试用户");

        when(roomRepository.existsByCode(anyString())).thenReturn(false);
        when(roomRepository.save(any(Room.class))).thenAnswer(inv -> {
            Room r = inv.getArgument(0);
            r.setId(1L);
            return r;
        });
        when(roomMemberRepository.save(any(RoomMember.class))).thenAnswer(inv -> inv.getArgument(0));

        Room result = roomService.createRoom(request);
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(6, result.getCode().length());
        assertFalse(result.getIsClosed());
        assertNotNull(result.getExpireAt());
        verify(roomMemberRepository).save(any(RoomMember.class));
    }

    @Test
    public void testCreateRoom_DefaultNickname() {
        ReflectionTestUtils.setField(roomService, "roomExpireMinutes", 30);

        CreateRoomRequest request = new CreateRoomRequest();
        request.setCreatorId("device-abcd1234");
        request.setLatitude(39.9);
        request.setLongitude(116.4);

        when(roomRepository.existsByCode(anyString())).thenReturn(false);
        when(roomRepository.save(any(Room.class))).thenAnswer(inv -> {
            Room r = inv.getArgument(0);
            r.setId(2L);
            return r;
        });
        when(roomMemberRepository.save(any(RoomMember.class))).thenAnswer(inv -> inv.getArgument(0));

        roomService.createRoom(request);

        verify(roomMemberRepository).save(argThat(member ->
                member.getNickname().equals("用户1234")
        ));
    }

    @Test
    public void testJoinRoom_WhenCodeExists() {
        ReflectionTestUtils.setField(roomService, "roomExpireMinutes", 30);
        ReflectionTestUtils.setField(roomService, "maxDistanceMeters", 200);

        Room mockRoom = new Room();
        mockRoom.setId(10L);
        mockRoom.setCode("987654");
        mockRoom.setCreatorId("creator-abc");
        mockRoom.setIsClosed(false);
        mockRoom.setExpireAt(LocalDateTime.now().plusMinutes(30));

        when(roomRepository.findByCodeAndIsClosedFalse("987654"))
                .thenReturn(Optional.of(mockRoom));
        when(roomRepository.save(any(Room.class))).thenReturn(mockRoom);
        when(roomMemberRepository.findByRoomIdAndDeviceId(10L, "joiner-xyz"))
                .thenReturn(Optional.empty());
        when(roomMemberRepository.save(any(RoomMember.class))).thenAnswer(inv -> inv.getArgument(0));

        JoinRoomRequest request = new JoinRoomRequest();
        request.setCode("987654");
        request.setUserId("joiner-xyz");
        request.setLatitude(39.9);
        request.setLongitude(116.4);
        request.setNickname("加入者");

        Optional<Room> result = roomService.joinRoom(request);
        assertTrue(result.isPresent());
        assertEquals("987654", result.get().getCode());
        verify(roomMemberRepository).save(any(RoomMember.class));
    }

    @Test
    public void testJoinRoom_WhenCodeNotExists() {
        when(roomRepository.findByCodeAndIsClosedFalse("000000"))
                .thenReturn(Optional.empty());

        JoinRoomRequest request = new JoinRoomRequest();
        request.setCode("000000");
        request.setUserId("any-user");

        Optional<Room> result = roomService.joinRoom(request);
        assertFalse(result.isPresent());
    }

    @Test
    public void testJoinRoom_TooFar() {
        ReflectionTestUtils.setField(roomService, "roomExpireMinutes", 30);
        ReflectionTestUtils.setField(roomService, "maxDistanceMeters", 200);

        Room mockRoom = new Room();
        mockRoom.setId(11L);
        mockRoom.setCode("111111");
        mockRoom.setCreatorId("creator");
        mockRoom.setLatitude(39.9);
        mockRoom.setLongitude(116.4);
        mockRoom.setExpireAt(LocalDateTime.now().plusMinutes(30));

        when(roomRepository.findByCodeAndIsClosedFalse("111111"))
                .thenReturn(Optional.of(mockRoom));

        JoinRoomRequest request = new JoinRoomRequest();
        request.setCode("111111");
        request.setUserId("far-user");
        request.setLatitude(40.0);
        request.setLongitude(117.0);

        Optional<Room> result = roomService.joinRoom(request);
        assertFalse(result.isPresent());
    }

    @Test
    public void testGetRoomMembers_UpdatesLastActive() {
        RoomMember member = new RoomMember();
        member.setRoomId(1L);
        member.setDeviceId("device-1");
        member.setNickname("用户A");
        member.setLastActiveAt(LocalDateTime.now());

        when(roomMemberRepository.findByRoomIdAndDeviceId(1L, "device-1"))
                .thenReturn(Optional.of(member));
        when(roomMemberRepository.saveAndFlush(any(RoomMember.class))).thenReturn(member);
        when(roomMemberRepository.findByRoomIdAndLastActiveAtAfter(eq(1L), any(LocalDateTime.class)))
                .thenReturn(List.of(member));
        when(roomRepository.findById(1L)).thenReturn(Optional.of(new Room()));

        RoomMembersResponseDto result = roomService.getRoomMembers(1L, "device-1");
        assertNotNull(result);
        assertEquals(1, result.getMembers().size());
        assertEquals("用户A", result.getMembers().get(0).getNickname());
        verify(roomMemberRepository).saveAndFlush(any(RoomMember.class));
    }

    @Test
    public void testUpdateNickname() {
        RoomMember member = new RoomMember();
        member.setRoomId(1L);
        member.setDeviceId("device-1");
        member.setNickname("旧昵称");

        when(roomMemberRepository.findByRoomIdAndDeviceId(1L, "device-1"))
                .thenReturn(Optional.of(member));
        when(roomMemberRepository.save(any(RoomMember.class))).thenReturn(member);

        UpdateNicknameRequest request = new UpdateNicknameRequest();
        request.setRoomId(1L);
        request.setDeviceId("device-1");
        request.setNickname("新昵称");

        boolean result = roomService.updateNickname(request);
        assertTrue(result);
        assertEquals("新昵称", member.getNickname());
    }

    @Test
    public void testUpdateNickname_MemberNotFound() {
        when(roomMemberRepository.findByRoomIdAndDeviceId(1L, "unknown"))
                .thenReturn(Optional.empty());

        UpdateNicknameRequest request = new UpdateNicknameRequest();
        request.setRoomId(1L);
        request.setDeviceId("unknown");
        request.setNickname("新昵称");

        boolean result = roomService.updateNickname(request);
        assertFalse(result);
    }

    @Test
    public void testSetAllowMemberUpload_ByOwner() {
        Room room = new Room();
        room.setId(1L);
        room.setCreatorId("owner-device");
        room.setAllowMemberUpload(false);

        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(roomRepository.save(any(Room.class))).thenReturn(room);

        boolean result = roomService.setAllowMemberUpload(1L, "owner-device", true);
        assertTrue(result);
        assertTrue(room.getAllowMemberUpload());
    }

    @Test
    public void testSetAllowMemberUpload_ByNonOwner() {
        Room room = new Room();
        room.setId(1L);
        room.setCreatorId("owner-device");
        room.setAllowMemberUpload(false);

        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));

        boolean result = roomService.setAllowMemberUpload(1L, "other-device", true);
        assertFalse(result);
        assertFalse(room.getAllowMemberUpload());
    }

    @Test
    public void testCloseRoom_DeletesMembers() {
        Room room = new Room();
        room.setId(1L);
        room.setCreatorId("owner");
        room.setIsClosed(false);

        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(roomRepository.save(any(Room.class))).thenReturn(room);

        boolean result = roomService.closeRoom(1L);
        assertTrue(result);
        assertTrue(room.getIsClosed());
        verify(roomMemberRepository).deleteByRoomId(1L);
        verify(fileService).deleteAllFilesByRoomId(1L);
    }
}
