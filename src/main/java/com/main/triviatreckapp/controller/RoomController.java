package com.main.triviatreckapp.controller;


import com.main.triviatreckapp.entities.Message;
import com.main.triviatreckapp.entities.Room;
import com.main.triviatreckapp.service.ChatService;
import com.main.triviatreckapp.service.RoomService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/rooms")
@CrossOrigin(origins = "http://localhost:5173")
public class RoomController {
    private final RoomService roomService;
    private final ChatService chatService;
    public RoomController(RoomService roomService, ChatService chatService) { this.roomService = roomService; this.chatService = chatService;}

    @MessageMapping("/join/{roomId}")
    @SendTo("/room/{roomId}")
    public Room joinRoom(@DestinationVariable String roomId, @RequestBody String user) {
            System.out.println("joining.... : ");
            Room room = roomService.getOrCreateRoom(roomId);
            roomService.addParticipant(roomId, user);
            System.out.println("room created.... : ");
            System.out.println(room.toString());

            return room;
    }

    @MessageMapping("/sendMessage/{roomId}")
    @SendTo("/room/{roomId}")
    public Room sendMessageToRoom(@DestinationVariable String roomId, @RequestBody Message chatMessage) {
        System.out.println("sending message.... : ");
        System.out.println(chatMessage.toString());
        System.out.println("room id : " + roomId);
        Optional<Room> room = roomService.getRoom(roomId);
        System.out.println("room : " + (room.isPresent()));
        room.ifPresent(r -> System.out.println("Participants : " + r.getParticipants()));

        if (room.isPresent()) {
            Message savedMessage = chatService.saveMessage(
                    roomId, chatMessage.getSender(), chatMessage.getContent());
            room.get().getMessages().add(savedMessage);
            return roomService.saveRoom(room.get());
        } else {
            throw new RuntimeException("room not found !!");
        }
    }

    @MessageMapping("/leave/{roomId}")
    @SendTo("/room/{roomId}")
    public Optional<Room> leaveRoom(@DestinationVariable String roomId, @RequestBody String user) {
        roomService.removeParticipant(roomId, user);
        if(roomService.getRoom(roomId).isPresent() && roomService.getRoom(roomId).get().getParticipants().isEmpty()){
            roomService.deleteRoom(roomId);
        }
        return roomService.getRoom(roomId);
    }
}

