package com.main.triviatreckapp.controller;

import com.main.triviatreckapp.Request.PlayerJokerRequest;
import com.main.triviatreckapp.Request.StartGameRequest;
import com.main.triviatreckapp.dto.PlayerAnswerDTO;
import com.main.triviatreckapp.dto.QuizGameDTO;
import com.main.triviatreckapp.service.QuizGameService;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Optional;

@Controller
public class QuizGameController {
  private final QuizGameService gameService;
    public QuizGameController(QuizGameService gameService) {
      this.gameService = gameService;
    }


    // Lancement d'une partie dans la room
  @MessageMapping("/game/startQuiz/{gameId}")
  @SendTo("/game/{gameId}")
  public QuizGameDTO startQuizGame(@DestinationVariable String gameId, @Payload StartGameRequest payload) {
      return gameService.startQuizGameDTO(gameId, payload);

  }


    @MessageMapping("/game/join/{gameId}")
    @SendTo("/game/{gameId}")
    public QuizGameDTO joinGame(@DestinationVariable String gameId, @Payload Long participantId) {
      return gameService.enterQuizGame(gameId, participantId);
    }

    @MessageMapping("/game/leave/{gameId}")
    @SendTo("/game/{gameId}")
    public Optional<QuizGameDTO> leaveGame(@DestinationVariable String gameId, @Payload Long participantId) {
        return gameService.removeParticipantFromGame(gameId, participantId);

    }

    // Réception d'une réponse d'un joueur
  @MessageMapping("/game/answer/{gameId}")
  @SendTo("/game/{gameId}")
  public QuizGameDTO processAnswer(@DestinationVariable String gameId,
                                  @Payload PlayerAnswerDTO playerAnswer) {
      return gameService.processAnswerDTO(gameId, playerAnswer);
  }

  @GetMapping("/games/{gameId}")
  @ResponseBody
  public QuizGameDTO getQuizGame(@PathVariable String gameId) {
      return gameService.getQuizGameDTO(gameId);
  }


  @MessageMapping("/game/joker/{gameId}")
  @SendTo("/game/{gameId}")
  public void processJoker(@DestinationVariable String gameId,
                                   @Payload PlayerJokerRequest joker) {
    gameService.processJoker(gameId, joker);
  }
}
