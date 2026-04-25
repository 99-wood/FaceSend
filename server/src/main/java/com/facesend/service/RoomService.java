package com.facesend.service;

import com.facesend.dto.CreateRoomRequest;
import com.facesend.dto.JoinRoomRequest;
import com.facesend.entity.Room;
import com.facesend.repository.RoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@Service
public class RoomService {

    @Autowired
    private RoomRepository roomRepository;

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
        return roomRepository.save(room);
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
        return Optional.of(room);
    }

    @Transactional
    public boolean closeRoom(Long roomId) {
        Optional<Room> roomOpt = roomRepository.findById(roomId);
        if (roomOpt.isPresent()) {
            Room room = roomOpt.get();
            room.setIsClosed(true);
            roomRepository.save(room);
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
                fileService.deleteAllFilesByRoomId(room.getId());
            } catch (Exception e) {
                System.err.println("清理房间 " + room.getId() + " 失败: " + e.getMessage());
            }
        }
        if (!expiredRooms.isEmpty()) {
            System.out.println("已清理 " + expiredRooms.size() + " 个过期房间");
        }
    }
}
