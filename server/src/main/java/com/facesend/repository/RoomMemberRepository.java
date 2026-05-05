package com.facesend.repository;

import com.facesend.entity.RoomMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RoomMemberRepository extends JpaRepository<RoomMember, Long> {
    List<RoomMember> findByRoomId(Long roomId);
    List<RoomMember> findByRoomIdAndLastActiveAtAfter(Long roomId, LocalDateTime threshold);
    Optional<RoomMember> findByRoomIdAndDeviceId(Long roomId, String deviceId);
    @Modifying
    void deleteByRoomId(Long roomId);
}
