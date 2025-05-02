package com.main.triviatreckapp.controller;


import com.main.triviatreckapp.dto.MessageDTO;
import com.main.triviatreckapp.dto.RoomDTO;
import com.main.triviatreckapp.entities.Message;
import com.main.triviatreckapp.entities.Room;
import com.main.triviatreckapp.service.ChatService;
import com.main.triviatreckapp.service.RoomService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
@CrossOrigin(origins = "http://localhost:5173")
public class RoomController {
    private final RoomService roomService;
    private final ChatService chatService;
    public RoomController(RoomService roomService, ChatService chatService) { this.roomService = roomService; this.chatService = chatService;}

    @MessageMapping("/join/{roomId}")
    @SendTo("/chatroom/{roomId}")
    @Transactional
    public RoomDTO joinRoom(@DestinationVariable String roomId, @Payload String user) {
            Room room = roomService.getOrCreateRoom(roomId);
            roomService.addParticipant(roomId, user);
        return convertRoomToDTO(room, roomId)
                ;
    }

    @MessageMapping("/sendMessage/{roomId}")
    @SendTo("/chatroom/{roomId}")
    @Transactional
    public RoomDTO sendMessageToRoom(@DestinationVariable String roomId, @Payload Message chatMessage) {
        Optional<Room> room = roomService.getRoom(roomId);

        if (room.isPresent()) {
            Message savedMessage = chatService.saveMessage(
                    roomId, chatMessage.getSender(), chatMessage.getContent());
            room.get().getMessages().add(savedMessage);
            return convertRoomToDTO(roomService.saveRoom(room.get()), roomId);
        } else {
            throw new RuntimeException("room not found !!");
        }
    }

    @MessageMapping("/leave/{roomId}")
    @SendTo("/chatroom/{roomId}")
    @Transactional
    public Optional<RoomDTO> leaveRoom(@DestinationVariable String roomId, @Payload String user) {
        roomService.removeParticipant(roomId, user);
        Optional<Room> room = roomService.getRoom(roomId);
        if(room.isPresent()) {
            if(room.get().getParticipants().isEmpty()) {
                roomService.deleteRoom(roomId);
            }
        }
        return room.map(r -> convertRoomToDTO(r, roomId)).or(Optional::empty);

    }

    public RoomDTO convertRoomToDTO(Room room, String roomId) {
        List<String> participantsDTO = new ArrayList<>(room.getParticipants());

        List<MessageDTO> messagesDTO = room.getMessages().stream()
                .map(message ->
                        new MessageDTO(message.getRoomId(),
                                message.getSender(),
                                message.getContent()))
                .toList();

        return new RoomDTO(roomId, participantsDTO, messagesDTO);
    }
}

