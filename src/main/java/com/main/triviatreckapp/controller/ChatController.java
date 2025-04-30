package com.main.triviatreckapp.controller;

import com.main.triviatreckapp.model.Message;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class ChatController {

    private final SimpMessagingTemplate simpMessagingTemplate;

    public ChatController(SimpMessagingTemplate simpMessagingTemplate) {
        this.simpMessagingTemplate = simpMessagingTemplate;
    }

    @MessageMapping("/message") // /app/message
    private Message receivePublicMessage(@Payload Message message) {
        simpMessagingTemplate.convertAndSend("/chatroom/"+message.getRoomId()+"/public", message);
        System.out.println("message received: " + message);
        return message;
    }

}
