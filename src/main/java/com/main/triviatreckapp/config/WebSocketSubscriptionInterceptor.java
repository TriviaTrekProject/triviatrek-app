package com.main.triviatreckapp.config;

import com.main.triviatreckapp.dto.QuizGameDTO;
import com.main.triviatreckapp.dto.RoomDTO;
import com.main.triviatreckapp.service.QuizGameService;
import com.main.triviatreckapp.service.RoomService;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

@Component
public class WebSocketSubscriptionInterceptor implements ChannelInterceptor {

    private final RoomService roomService;
    private final QuizGameService quizGameService;
    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketSubscriptionInterceptor(RoomService roomService, 
                                           QuizGameService quizGameService,
                                           SimpMessagingTemplate messagingTemplate) {
        this.roomService = roomService;
        this.quizGameService = quizGameService;
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.wrap(message);

        // Check if this is a subscription message
        if (accessor.getMessageType() == SimpMessageType.SUBSCRIBE) {
            String destination = accessor.getDestination();

            if (destination != null) {
                // Handle /chatroom/{roomId} subscriptions
                if (destination.startsWith("/chatroom/")) {
                    String roomId = extractId(destination, "/chatroom/");
                    if (roomId != null && roomService.getRoom(roomId).isPresent()) {
                        try {
                            // Get room data
                            RoomDTO roomDTO = roomService.getRoomDTO(roomId);

                            // Send room data to the subscriber
                            messagingTemplate.convertAndSend(destination, roomDTO);
                        } catch (Exception e) {
                            // Log error but don't block subscription
                            System.err.println("Error sending room data on subscription: " + e.getMessage());
                        }
                    }
                }
                // Handle /game/{gameId} subscriptions
                else if (destination.startsWith("/game/")) {
                    String gameId = extractId(destination, "/game/");
                    if (gameId != null && quizGameService.getGame(gameId).isPresent()) {
                        try {
                            // Get game data
                            QuizGameDTO gameDTO = quizGameService.getQuizGameDTO(gameId);

                            // Send game data to the subscriber
                            messagingTemplate.convertAndSend(destination, gameDTO);
                        } catch (Exception e) {
                            // Log error but don't block subscription
                            System.err.println("Error sending game data on subscription: " + e.getMessage());
                        }
                    }
                }
            }
        }

        return message;
    }

    private String extractId(String destination, String prefix) {
        if (destination.startsWith(prefix)) {
            return destination.substring(prefix.length());
        }
        return null;
    }
}
