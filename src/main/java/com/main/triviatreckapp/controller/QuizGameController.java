package com.main.triviatreckapp.controller;

import com.main.triviatreckapp.dto.PlayerAnswerDTO;
import com.main.triviatreckapp.dto.QuizGameDTO;
import com.main.triviatreckapp.entities.QuizGame;
import com.main.triviatreckapp.entities.Room;
import com.main.triviatreckapp.service.QuizGameService;

import com.main.triviatreckapp.service.RoomService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.Optional;

@Controller
@CrossOrigin
public class QuizGameController {
  private final QuizGameService gameService;
    private final RoomService roomService;

    public QuizGameController(QuizGameService gameService, RoomService roomService) {
      this.gameService = gameService;
        this.roomService = roomService;
    }

  // Lancement d'une partie dans la room
  @MessageMapping("/game/start/{roomId}")
  @SendTo("/game/{gameId}")
  public QuizGameDTO startGame(@DestinationVariable String roomId) {
      QuizGame game = gameService.createOrRestartGame(roomId);
      Optional<Room> room = roomService.getRoom(roomId);
      room.ifPresent(rm -> rm.setQuizGame(game));

      return gameService.toDTO(game);
  }

    @MessageMapping("/game/join/{gameId}")
    @SendTo("/game/{gameId}")
    @Transactional
    public QuizGameDTO joinGame(@DestinationVariable String gameId, @Payload String user) {
        System.out.println("User " + user + " joining game " + gameId);
        QuizGame updated = gameService.addParticipant(gameId, user);
        return gameService.toDTO(updated);

    }

    @MessageMapping("/game/leave/{gameId}")
    @SendTo("/game/{gameId}")
    @Transactional
    public QuizGameDTO leaveGame(@DestinationVariable String gameId, @Payload String user) {
        System.out.println("User " + user + " leaving game " + gameId);
        QuizGame updated = gameService.removeParticipant(gameId, user);
        return gameService.toDTO(updated);

    }

  // Réception d'une réponse d'un joueur
  @MessageMapping("/game/answer/{gameId}")
  @SendTo("/game/{gameId}")
  public QuizGameDTO processAnswer(@DestinationVariable String gameId,
                                  @Payload PlayerAnswerDTO playerAnswer) {
      QuizGame game = gameService.processAnswer(gameId, playerAnswer);
      return gameService.toDTO(game);
  }

  // Récupération de l'état actuel du jeu
  @MessageMapping("/game/status/{gameId}")
  @SendTo("/game/{gameId}")
  public QuizGameDTO getGameStatus(@DestinationVariable String gameId) {
      QuizGame game = gameService.getGame(gameId);
      return gameService.toDTO(game);
  }

}

