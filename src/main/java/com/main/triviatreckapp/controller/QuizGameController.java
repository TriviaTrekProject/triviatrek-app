package com.main.triviatreckapp.controller;

import com.main.triviatreckapp.dto.PlayerAnswerDTO;
import com.main.triviatreckapp.dto.QuizGameDTO;
import com.main.triviatreckapp.entities.QuizGame;
import com.main.triviatreckapp.service.QuizGameService;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;

@Controller
@CrossOrigin
public class QuizGameController {
  private final QuizGameService gameService;

  public QuizGameController(QuizGameService gameService) {
      this.gameService = gameService;
  }

  // Lancement d'une partie dans la room
  @MessageMapping("/game/start/{roomId}")
  @SendTo("/game/{roomId}")
  public QuizGameDTO startGame(@DestinationVariable String roomId) {
      QuizGame game = gameService.createOrRestartGame(roomId);
      return gameService.toDTO(game);
  }

  // Réception d'une réponse d'un joueur
  @MessageMapping("/game/answer/{roomId}")
  @SendTo("/game/{roomId}")
  public QuizGameDTO processAnswer(@DestinationVariable String roomId,
                                  @Payload PlayerAnswerDTO playerAnswer) {
      QuizGame game = gameService.processAnswer(roomId, playerAnswer);
      return gameService.toDTO(game);
  }

  // Récupération de l'état actuel du jeu
  @MessageMapping("/game/status/{roomId}")
  @SendTo("/game/{roomId}")
  public QuizGameDTO getGameStatus(@DestinationVariable String roomId) {
      QuizGame game = gameService.getGame(roomId);
      return gameService.toDTO(game);
  }

}

