package com.facesend.repository;

import com.facesend.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {
    Optional<Room> findByCodeAndIsClosedFalse(String code);
    List<Room> findByIsClosedFalseAndExpireAtBefore(LocalDateTime time);
    boolean existsByCode(String code);
}
