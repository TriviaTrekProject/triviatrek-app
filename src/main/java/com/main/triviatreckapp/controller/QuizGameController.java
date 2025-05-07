package com.main.triviatreckapp.controller;

import com.main.triviatreckapp.Request.StartGameRequest;
import com.main.triviatreckapp.dto.PlayerAnswerDTO;
import com.main.triviatreckapp.dto.QuizGameDTO;
import com.main.triviatreckapp.entities.QuizGame;
import com.main.triviatreckapp.service.QuizGameService;

import com.main.triviatreckapp.service.RoomService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;


@Controller
@CrossOrigin
public class QuizGameController {
  private final QuizGameService gameService;

    public QuizGameController(QuizGameService gameService, RoomService roomService, SimpMessagingTemplate messagingTemplate) {
      this.gameService = gameService;
    }


    // Lancement d'une partie dans la room
  @MessageMapping("/game/start/{gameId}")
  @SendTo("/game/{gameId}")
  public QuizGameDTO startGame(@DestinationVariable String gameId, @Payload StartGameRequest payload) {
      return gameService.getQuizGameDTO(gameId, payload);

  }


    @MessageMapping("/game/join/{gameId}")
    @SendTo("/game/{gameId}")
    public QuizGameDTO joinGame(@DestinationVariable String gameId, @Payload String user) {
      return gameService.enterQuizGame(gameId, user);
    }

  @MessageMapping("/game/leave/{gameId}")
    @SendTo("/game/{gameId}")
    public QuizGameDTO leaveGame(@DestinationVariable String gameId, @Payload String user) {
        return gameService.removeParticipantFromGame(gameId, user);

    }

    // Réception d'une réponse d'un joueur
  @MessageMapping("/game/answer/{gameId}")
  @SendTo("/game/{gameId}")
  public QuizGameDTO processAnswer(@DestinationVariable String gameId,
                                  @Payload PlayerAnswerDTO playerAnswer) {
      return gameService.processAnswerDTO(gameId, playerAnswer);
  }

}