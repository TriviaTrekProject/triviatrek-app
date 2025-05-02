package com.main.triviatreckapp.service;

import com.main.triviatreckapp.dto.MessageDTO;
import com.main.triviatreckapp.dto.RoomDTO;
import com.main.triviatreckapp.entities.Room;
import com.main.triviatreckapp.repository.RoomRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class RoomService {
    private final RoomRepository roomRepo;
    public RoomService(RoomRepository roomRepo) { this.roomRepo = roomRepo; }

    public Optional<Room> getRoom(String roomId) {
        return roomRepo.findByRoomId(roomId);
    }
    public Room getOrCreateRoom(String roomId) {
        return roomRepo.findByRoomId(roomId)
                .orElseGet(() ->
                    createRoom(roomId)
                );
    }

    public void deleteRoom(String roomId) {
        roomRepo.deleteByRoomId(roomId);
    }
    public Room saveRoom(Room room) {
        return roomRepo.save(room);
    }

    public void addParticipant(String roomId, String user) {
        Room room = getOrCreateRoom(roomId);
        room.getParticipants().add(user);
        roomRepo.save(room);
    }

    public void removeParticipant(String roomId, String user) {
        Room room = getRoom(roomId).orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));
        room.getParticipants().remove(user);
        roomRepo.save(room);
    }

    public boolean isParticipant(String roomId, String user) {
        return roomRepo.findByRoomId(roomId)
                .map(r -> r.getParticipants().contains(user))
                .orElse(false);
    }

    public Room createRoom(String roomId) {
        if (roomRepo.existsByRoomId(roomId)) {
            throw new IllegalArgumentException("La room existe déjà : " + roomId);
        }
        Room room = new Room();
        room.setRoomId(roomId);
        return roomRepo.save(room);
    }

}
