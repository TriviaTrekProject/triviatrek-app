package com.main.triviatreckapp.config;

import com.main.triviatreckapp.dto.RoomDTO;
import com.main.triviatreckapp.service.RoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompEventListener {

    private final RoomService roomService;   // votre logique métier
    private final SimpMessagingTemplate messagingTemplate;

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor sha = StompHeaderAccessor.wrap(event.getMessage());

        Map<String, Object> sessionAttributes = sha.getSessionAttributes();
        if (sessionAttributes != null && sessionAttributes.containsKey("roomId") && sessionAttributes.containsKey("username")) {
            String roomId = (String) sessionAttributes.get("roomId");
            String username = (String) sessionAttributes.get("username");
            roomService.removeParticipantAndCheckRoomStatus(roomId, username);

            if (roomId != null && roomService.getRoom(roomId).isPresent()) {
                try {
                    RoomDTO roomDTO = roomService.getRoomDTO(roomId);
                    messagingTemplate.convertAndSend("/chatroom/" + roomId, roomDTO);
                } catch (Exception e) {
                    System.err.println("Error removing user data on disconnect: " + e.getMessage());
                }
            }
        }
    }


    @EventListener
    public void onSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor sha = StompHeaderAccessor.wrap(event.getMessage());
        sha.getSessionId();
    }
}

