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
import java.util.Objects;

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

            if (roomId != null && roomService.getRoom(roomId)
                    .filter(room -> !room.getParticipants().isEmpty())
                    .isPresent()) {
                try {
                    roomService.removeParticipantAndCheckRoomStatus(roomId, username);
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
        if (Objects.requireNonNull(sha.getDestination()).startsWith("/chatroom/")) {
            String roomId = extractId(sha.getDestination());
            if (roomId != null && roomService.getRoom(roomId).isPresent()) {
                try {
                    // Get room data
                    RoomDTO roomDTO = roomService.getRoomDTO(roomId);

                    // Send room data to the subscriber
                    messagingTemplate.convertAndSend(sha.getDestination(), roomDTO);
                } catch (Exception e) {
                    // Log error but don't block subscription
                    System.err.println("Error sending room data on subscription: " + e.getMessage());
                }
            }
        }
    }

    private String extractId(String destination) {
        if (destination.startsWith("/chatroom/")) {
            return destination.substring("/chatroom/".length());
        }
        return null;
    }
}

