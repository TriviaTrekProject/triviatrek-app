package com.main.triviatreckapp.controller;

import com.main.triviatreckapp.dto.PlayerAnswerDTO;
import com.main.triviatreckapp.dto.QuizGameDTO;
import com.main.triviatreckapp.entities.QuizGame;
import com.main.triviatreckapp.entities.Room;
import com.main.triviatreckapp.service.QuizGameService;

import com.main.triviatreckapp.service.RoomService;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

@Controller
@CrossOrigin
public class QuizGameController {
  private final QuizGameService gameService;
    private final RoomService roomService;
    private final SimpMessagingTemplate messagingTemplate;

    public QuizGameController(QuizGameService gameService, RoomService roomService, SimpMessagingTemplate messagingTemplate) {
      this.gameService = gameService;
        this.roomService = roomService;
        this.messagingTemplate = messagingTemplate;
    }

  // Lancement d'une partie dans la room
  @MessageMapping("/game/start/{gameId}")
  @Transactional
  public void startGame(@DestinationVariable String gameId, @RequestParam String roomId, @RequestParam String user) {
      System.out.println("launching game " + gameId + " in room " + roomId + "...");
      Room room = roomService.getRoom(roomId).orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));
      QuizGame game = gameService.createGame(gameId, room);
      gameService.addParticipant(gameId, user);
      room.setQuizGame(game);
      roomService.saveRoom(room);

      String destination = "/game/" + game.getGameId();
      System.out.println("Sending game"+ game.getGameId() +" to " + destination);
      messagingTemplate.convertAndSend(destination, gameService.toDTO(game));

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
  @Transactional
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