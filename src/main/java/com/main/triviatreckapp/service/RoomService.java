package com.main.triviatreckapp.service;

import com.main.triviatreckapp.dto.MessageDTO;
import com.main.triviatreckapp.dto.RoomDTO;
import com.main.triviatreckapp.entities.Message;
import com.main.triviatreckapp.entities.Room;
import com.main.triviatreckapp.repository.RoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class RoomService {
    private final RoomRepository roomRepo;
    private final ChatService chatService;

    public RoomService(RoomRepository roomRepo, ChatService chatService) { this.roomRepo = roomRepo;
        this.chatService = chatService;
    }
    @Transactional(readOnly = true)
    public Optional<Room> getRoom(String roomId) {
        return roomRepo.findByRoomId(roomId);
    }
    @Transactional
    public Room getOrCreateRoom(String roomId) {
        return roomRepo.findByRoomId(roomId)
                .orElseGet(() ->
                    createRoom(roomId)
                );
    }
    @Transactional
    public void deleteRoom(String roomId) {
        roomRepo.deleteByRoomId(roomId);
    }

    @Transactional
    public Room saveRoom(Room room) {
        return roomRepo.save(room);
    }


    @Transactional
    public void removeParticipant(String roomId, String user) {
        Room room = getRoom(roomId).orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));
        room.getParticipants().remove(user);
        roomRepo.save(room);
    }

    @Transactional
    public Room createRoom(String roomId) {
        if (roomRepo.existsByRoomId(roomId)) {
            throw new IllegalArgumentException("La room existe déjà : " + roomId);
        }
        Room room = new Room();
        room.setRoomId(roomId);
        return roomRepo.save(room);
    }

    public RoomDTO convertRoomToDTO(Room room, String roomId) {
        List<String> participantsDTO = new ArrayList<>(room.getParticipants());
        String gameId = room.getQuizGame() != null ? room.getQuizGame().getGameId() : UUID.randomUUID().toString();

        List<MessageDTO> messagesDTO = room.getMessages().stream()
                .map(message ->
                        new MessageDTO(message.getRoomId(),
                                message.getSender(),
                                message.getContent()))
                .toList();
        return new RoomDTO(roomId, participantsDTO, messagesDTO, gameId, room.isActiveGame());
    }

    @Transactional
    public RoomDTO addUserToRoom(String roomId, String user) {

        // 1) Récupération de la room avec verrou d’écriture
        Room room = roomRepo.findByRoomIdForUpdate(roomId)
                .orElseGet(() -> createRoom(roomId));

        // 2) Idempotence : on n’ajoute que si nécessaire
        if (!room.getParticipants().contains(user)) {
            Message sysMsg = chatService.saveMessage(
                    roomId,
                    "SYSTEM",                       // expéditeur technique
                     user + " a rejoint la room");

            room.getMessages().add(sysMsg);
            room.addParticipant(user);
            roomRepo.save(room);
        }

        // 3) Conversion du résultat
        return convertRoomToDTO(room, roomId);
    }

    @Transactional
    public RoomDTO submitMessageToRoom(String roomId, Message chatMessage) {
        Optional<Room> room = getRoom(roomId);

        if (room.isPresent()) {
            Message savedMessage = chatService.saveMessage(
                    roomId, chatMessage.getSender(), chatMessage.getContent());
            room.get().getMessages().add(savedMessage);
            return convertRoomToDTO(saveRoom(room.get()), roomId);
        } else {
            throw new RuntimeException("room not found !!");
        }
    }

    @Transactional
    public Optional<RoomDTO> removeParticipantAndCheckRoomStatus(String roomId, String user) {
        // 1) Verrou pessimiste pour éviter les accès concurrents
        Room room = roomRepo.findByRoomIdForUpdate(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));

        if(room.getParticipants().contains(user))
        {
            removeParticipant(roomId, user);
            room.getParticipants().remove(user);
            Message sysMsg = chatService.saveMessage(
                    roomId,
                    "SYSTEM",
                    user + " a quitté la room");
            room.getMessages().add(sysMsg);

        }

        if(room.getParticipants().isEmpty()) {
                deleteRoom(roomId);
        }
        roomRepo.save(room);
        return Optional.ofNullable(convertRoomToDTO(room, roomId));
    }
    @Transactional
    public RoomDTO getRoomDTO(String roomId) {
        return getRoom(roomId).map(room -> convertRoomToDTO(room, roomId)).orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));
    }

}
