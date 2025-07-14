package com.main.triviatreckapp.service;

import com.main.triviatreckapp.dto.MessageDTO;
import com.main.triviatreckapp.dto.ParticipantDTO;
import com.main.triviatreckapp.dto.RoomDTO;
import com.main.triviatreckapp.entities.Message;
import com.main.triviatreckapp.entities.Participant;
import com.main.triviatreckapp.entities.Room;
import com.main.triviatreckapp.repository.ParticipantRepository;
import com.main.triviatreckapp.repository.RoomRepository;
import jakarta.servlet.http.Part;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class RoomService {
    private final RoomRepository roomRepo;
    private final ChatService chatService;
    private final ParticipantRepository participantRepo;

    public RoomService(RoomRepository roomRepo, ChatService chatService, ParticipantRepository participantRepo) { this.roomRepo = roomRepo;
        this.chatService = chatService;
        this.participantRepo = participantRepo;
    }

    /**
     * Génère un nom unique en ajoutant (2), (3)... tant que nécessaire.
     */
    private String generateUniqueName(Collection<String> existing, String base) {
        String candidate = base;
        int suffix = 2;
        while (existing.contains(candidate)) {
            candidate = base + "(" + suffix + ")";
            suffix++;
        }
        return candidate;
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
    public void removeParticipant(String roomId, String username) {
        Room room = roomRepo.findByRoomId(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room introuvable"));
        room.getParticipants().removeIf(p -> p.getUsername().equals(username));
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

    public RoomDTO convertRoomToDTO(Room room) {
        List<ParticipantDTO> participantsDTO = new ArrayList<>();
        room.getParticipants().forEach(participant -> {
            participantsDTO.add(new ParticipantDTO(participant.getId(), participant.getUsername(), participant.getDelaiReponse(), null));
        });
                ;
        List<MessageDTO> msgs = room.getMessages().stream()
                .map(m -> new MessageDTO(m.getRoomId(), m.getSender(), m.getContent()))
                .toList();
        String gameId = room.getQuizGame() != null
                ? room.getQuizGame().getGameId()
                : UUID.randomUUID().toString();

        return new RoomDTO(room.getRoomId(),
                participantsDTO,
                msgs,
                gameId,
                room.isActiveGame());
    }


    /**
     * Renvoie un pseudo unique dans la room (suffixe (2),(3)…)
     * même si la room n'existe pas encore.
     */
    @Transactional(readOnly = true)
    public String getUniqueUserName(String roomId, String desiredUser) {
        // récupère la room si elle existe
        Room room = roomRepo.findByRoomId(roomId).orElse(null);
        Collection<String> participants = (room != null)
                ? room.getParticipants().stream().map(Participant::getUsername).toList()
                : List.of();
        return generateUniqueName(participants, desiredUser);
    }


    @Transactional
    public RoomDTO addUserToRoom(String roomId, String username, String tempUuid) {
        Room room = roomRepo.findByRoomIdForUpdate(roomId)
                .orElseGet(() -> createRoom(roomId));

        // création ou récupération du Participant
        Participant p = participantRepo.findByUsername(username)
                .orElseGet(() -> participantRepo.save(new Participant(username, 0)));

        if (room.getParticipants().stream().noneMatch(x -> x.getUsername().equals(username))) {
            Message sysMsg = chatService.saveMessage(roomId, "SYSTEM", username + " a rejoint la room");
            room.getMessages().add(sysMsg);
            room.addParticipant(p);
            roomRepo.save(room);
        }
        RoomDTO roomDTO = convertRoomToDTO(room);

        // On reconstruit la liste en injectant le tempId là où il faut
        List<ParticipantDTO> updated = roomDTO.getParticipants().stream()
                .peek(part -> {
                    if (part.getUsername().equals(username)) {
                        part.setTempId(tempUuid);
                    }
                })
                .toList();

        // On remplace l’ancienne liste par la nouvelle
        roomDTO.setParticipants(updated);

        return roomDTO;

    }



    @Transactional
    public RoomDTO submitMessageToRoom(String roomId, Message chatMessage) {
        Optional<Room> room = getRoom(roomId);

        if (room.isPresent()) {
            Message savedMessage = chatService.saveMessage(
                    roomId, chatMessage.getSender(), chatMessage.getContent());
            room.get().getMessages().add(savedMessage);
            return convertRoomToDTO(saveRoom(room.get()));
        } else {
            throw new RuntimeException("room not found !!");
        }
    }

    @Transactional
    public Optional<RoomDTO> removeParticipantAndCheckRoomStatus(String roomId, String username) {
        // Utilisation d’un verrou pessimiste pour éviter les accès concurrents
        Optional<Room> optionalRoom = roomRepo.findByRoomIdForUpdate(roomId);
        if (optionalRoom.isEmpty()) {
            System.err.println("Room not found: " + roomId + ". Aucun participant à supprimer.");
            return Optional.empty();
        }
        Room room = optionalRoom.get();

        if (room.getParticipants().stream().anyMatch(p -> p.getUsername().equals(username))) {
            Participant participant = room.getParticipants().stream().filter(p -> p.getUsername().equals(username)).findAny().orElseThrow();
            room.removeParticipant(participant);
            Message sysMsg = chatService.saveMessage(
                    roomId,
                    "SYSTEM",
                    participant.getUsername() + " a quitté la room");
            room.getMessages().add(sysMsg);
        }

        if (room.getParticipants().isEmpty()) {
            deleteRoom(roomId);
            return Optional.empty();
        } else {
            roomRepo.save(room);
            return Optional.ofNullable(convertRoomToDTO(room));
        }
    }

    @Transactional
    public RoomDTO getRoomDTO(String roomId) {
        return getRoom(roomId).map(this::convertRoomToDTO).orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));
    }

}
