package com.main.triviatreckapp.repository;

import com.main.triviatreckapp.entities.Room;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {
    Optional<Room> findByRoomId(String roomId);
    boolean existsByRoomId(String roomId);

    void deleteByRoomId(String roomId);

    /** Lecture avec verrou pessimiste  */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Room r where r.roomId = :roomId")
    Optional<Room> findByRoomIdForUpdate(String roomId);

}
