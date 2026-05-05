package com.facesend.service;

import com.facesend.dto.CreateRoomRequest;
import com.facesend.dto.JoinRoomRequest;
import com.facesend.dto.RoomMemberDto;
import com.facesend.dto.RoomMembersResponseDto;
import com.facesend.dto.UpdateNicknameRequest;
import com.facesend.entity.Room;
import com.facesend.entity.RoomMember;
import com.facesend.repository.RoomMemberRepository;
import com.facesend.repository.RoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

@Service
public class RoomService {

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private RoomMemberRepository roomMemberRepository;

    @Autowired
    private FileService fileService;

    @Value("${facesend.room-expire-minutes}")
    private int roomExpireMinutes;

    @Value("${facesend.max-distance-meters}")
    private int maxDistanceMeters;

    private static final Random random = new Random();

    private String generateRandomCode() {
        String code;
        do {
            code = String.format("%06d", random.nextInt(1000000));
        } while (roomRepository.existsByCode(code));
        return code;
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000;
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    @Transactional
    public Room createRoom(CreateRoomRequest request) {
        Room room = new Room();
        room.setCode(generateRandomCode());
        room.setCreatorId(request.getCreatorId());
        room.setLatitude(request.getLatitude());
        room.setLongitude(request.getLongitude());
        room.setExpireAt(LocalDateTime.now().plusMinutes(roomExpireMinutes));
        room = roomRepository.save(room);

        RoomMember member = new RoomMember();
        member.setRoomId(room.getId());
        member.setDeviceId(request.getCreatorId());
        member.setNickname(request.getNickname() != null ? request.getNickname() : "用户" + request.getCreatorId().substring(Math.max(0, request.getCreatorId().length() - 4)));
        roomMemberRepository.save(member);

        return room;
    }

    @Transactional
    public Optional<Room> joinRoom(JoinRoomRequest request) {
        Optional<Room> roomOpt = roomRepository.findByCodeAndIsClosedFalse(request.getCode());
        if (roomOpt.isEmpty()) {
            return Optional.empty();
        }
        Room room = roomOpt.get();

        // GPS 距离校验
        if (room.getLatitude() != null && room.getLongitude() != null
                && request.getLatitude() != null && request.getLongitude() != null) {
            double distance = calculateDistance(
                    room.getLatitude(), room.getLongitude(),
                    request.getLatitude(), request.getLongitude()
            );
            if (distance > maxDistanceMeters) {
                return Optional.empty();
            }
        }

        room.setUpdatedAt(LocalDateTime.now());
        room.setExpireAt(LocalDateTime.now().plusMinutes(roomExpireMinutes));
        roomRepository.save(room);

        String nickname = request.getNickname() != null ? request.getNickname() : "用户" + request.getUserId().substring(Math.max(0, request.getUserId().length() - 4));
        Optional<RoomMember> existingMember = roomMemberRepository.findByRoomIdAndDeviceId(room.getId(), request.getUserId());
        if (existingMember.isPresent()) {
            RoomMember member = existingMember.get();
            member.setNickname(nickname);
            roomMemberRepository.save(member);
        } else {
            RoomMember member = new RoomMember();
            member.setRoomId(room.getId());
            member.setDeviceId(request.getUserId());
            member.setNickname(nickname);
            roomMemberRepository.save(member);
        }

        return Optional.of(room);
    }

    @Transactional
    public boolean closeRoom(Long roomId) {
        Optional<Room> roomOpt = roomRepository.findById(roomId);
        if (roomOpt.isPresent()) {
            Room room = roomOpt.get();
            room.setIsClosed(true);
            roomRepository.save(room);
            roomMemberRepository.deleteByRoomId(roomId);
            fileService.deleteAllFilesByRoomId(roomId);
            return true;
        }
        return false;
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void cleanupExpiredRooms() {
        LocalDateTime now = LocalDateTime.now();
        var expiredRooms = roomRepository.findByIsClosedFalseAndExpireAtBefore(now);
        for (Room room : expiredRooms) {
            room.setIsClosed(true);
            roomRepository.save(room);
            try {
                roomMemberRepository.deleteByRoomId(room.getId());
                fileService.deleteAllFilesByRoomId(room.getId());
            } catch (Exception e) {
                System.err.println("清理房间 " + room.getId() + " 失败: " + e.getMessage());
            }
        }
        if (!expiredRooms.isEmpty()) {
            System.out.println("已清理 " + expiredRooms.size() + " 个过期房间");
        }
    }

    @Transactional
    public RoomMembersResponseDto getRoomMembers(Long roomId, String deviceId) {
        // 更新调用者的活跃时间
        if (deviceId != null) {
            Optional<RoomMember> self = roomMemberRepository.findByRoomIdAndDeviceId(roomId, deviceId);
            if (self.isPresent()) {
                self.get().setLastActiveAt(LocalDateTime.now());
                roomMemberRepository.saveAndFlush(self.get());
            }
        }

        // 只返回 5 秒内活跃的成员
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(5);
        List<RoomMemberDto> members = roomMemberRepository.findByRoomIdAndLastActiveAtAfter(roomId, threshold).stream()
                .map(m -> new RoomMemberDto(m.getDeviceId(), m.getNickname(), m.getJoinedAt()))
                .collect(Collectors.toList());

        boolean allowUpload = roomRepository.findById(roomId)
                .map(r -> Boolean.TRUE.equals(r.getAllowMemberUpload()))
                .orElse(false);

        return new RoomMembersResponseDto(members, allowUpload);
    }

    @Transactional
    public boolean setAllowMemberUpload(Long roomId, String deviceId, boolean allow) {
        Optional<Room> roomOpt = roomRepository.findById(roomId);
        if (roomOpt.isPresent()) {
            Room room = roomOpt.get();
            if (room.getCreatorId().equals(deviceId)) {
                room.setAllowMemberUpload(allow);
                roomRepository.save(room);
                return true;
            }
        }
        return false;
    }

    @Transactional
    public boolean updateNickname(UpdateNicknameRequest request) {
        Optional<RoomMember> memberOpt = roomMemberRepository.findByRoomIdAndDeviceId(request.getRoomId(), request.getDeviceId());
        if (memberOpt.isPresent()) {
            RoomMember member = memberOpt.get();
            member.setNickname(request.getNickname());
            roomMemberRepository.save(member);
            return true;
        }
        return false;
    }
}
