package com.main.triviatreckapp.controller;

import com.main.triviatreckapp.model.GameMessage;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class GameController {

    @MessageMapping("/chat")
    @SendTo("/room")
    public GameMessage send(GameMessage message) {
        return message; // relaye à tous
    }
}
