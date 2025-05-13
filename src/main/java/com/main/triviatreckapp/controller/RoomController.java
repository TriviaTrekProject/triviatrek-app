package com.main.triviatreckapp.controller;

import com.main.triviatreckapp.dto.RoomDTO;
import com.main.triviatreckapp.entities.Message;
import com.main.triviatreckapp.service.RoomService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
@CrossOrigin(origins = "https://triviatrek.onrender.com")
@Controller
public class RoomController {
    private final RoomService roomService;
    public RoomController(RoomService roomService) { this.roomService = roomService;}

    @MessageMapping("/join/{roomId}")
    @SendTo("/chatroom/{roomId}")
    public RoomDTO joinRoom(@DestinationVariable String roomId, @Payload String user) {
        return roomService.addUserToRoom(roomId, user);
    }


    @MessageMapping("/sendMessage/{roomId}")
    @SendTo("/chatroom/{roomId}")
    public RoomDTO sendMessageToRoom(@DestinationVariable String roomId, @Payload Message chatMessage) {
        return roomService.submitMessageToRoom(roomId, chatMessage);
    }

    @MessageMapping("/leave/{roomId}")
    @SendTo("/chatroom/{roomId}")
    public Optional<RoomDTO> leaveRoom(@DestinationVariable String roomId, @Payload String user) {
        return roomService.removeParticipantAndCheckRoomStatus(roomId, user);

    }
    @GetMapping("/games/")
    public String[] getGames() {
        return new String[]{"quiz"};
    }



}

