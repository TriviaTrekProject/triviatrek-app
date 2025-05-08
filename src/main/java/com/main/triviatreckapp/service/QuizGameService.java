package com.main.triviatreckapp.service;

import com.main.triviatreckapp.Request.StartGameRequest;
import com.main.triviatreckapp.dto.PlayerAnswerDTO;
import com.main.triviatreckapp.dto.QuestionDTO;
import com.main.triviatreckapp.dto.QuizGameDTO;
import com.main.triviatreckapp.dto.ScoreDTO;
import com.main.triviatreckapp.entities.Question;
import com.main.triviatreckapp.entities.QuizGame;
import com.main.triviatreckapp.entities.Room;
import com.main.triviatreckapp.repository.QuestionRepository;
import com.main.triviatreckapp.repository.QuizGameRepository;
import com.main.triviatreckapp.repository.RoomRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class QuizGameService {
    private final QuestionRepository questionRepository;
    private final QuizGameRepository gameRepository;
    private final RoomService roomService;

    @Value("${quiz.questions-per-game:10}")
    private int questionsPerGame;

    @Value("${quiz.correct-answer-points:1}")
    private int correctAnswerPoints;

    public QuizGameService(QuestionRepository questionRepository,
                           QuizGameRepository gameRepository, RoomService roomService) {
        this.questionRepository = questionRepository;
        this.gameRepository = gameRepository;
        this.roomService = roomService;
    }

    /**
     * Crée ou redémarre une partie dans une salle
     * @param gameId identifiant de la salle
     * @return l'objet QuizGame créé
     */
    @Transactional
    public QuizGame createGame(String gameId, Room room) {

        QuizGame game = new QuizGame();


        List<Question> all = questionRepository.findAll();
        Collections.shuffle(all);
        if (all.size() <= 20) {
             all.forEach(game::addQuestion);
        }
        else {
            all.subList(0, 20).forEach(game::addQuestion);
    }


        // Créer un nouveau jeu
            game.setRoom(room);
            game.setGameId(gameId);
            game.setCurrentQuestionIndex(0);
            game.setScores(new HashMap<>());
            game.setFinished(false);

        return gameRepository.saveAndFlush(game);
    }

    /**
     * Traite la réponse d'un joueur à la question courante
     * @param gameId identifiant de la salle
     * @param playerAnswer DTO contenant l'identifiant du joueur et sa réponse
     * @return le jeu mis à jour
     */
    @Transactional
    public QuizGameDTO processAnswerDTO(String gameId, PlayerAnswerDTO playerAnswer) {
        Optional<QuizGame> opt = gameRepository.findByGameId(gameId);

        if (opt.isEmpty()) {
            return null;
        }

        QuizGame game = opt.get();
        if (game.isFinished()) {
            return toDTO(game);
        }

        Question current = game.getCurrentQuestion();
        if (current == null) {
            game.setFinished(true);
            return toDTO(gameRepository.save(game));
        }


        if (Objects.equals(playerAnswer.getAnswer(), current.getCorrectAnswer())) {
            game.addScore(playerAnswer.getPlayer(), correctAnswerPoints);
        }

        game.nextQuestion();
        return toDTO(gameRepository.save(game));
    }


    /**
     * Transforme un objet QuizGame en DTO pour l'envoi au frontend
     * @param game le jeu à convertir
     * @return DTO prêt à être envoyé
     */

    public QuizGameDTO toDTO(QuizGame game) {
        if (game == null) {
            return null;
        }
        QuizGameDTO dto = new QuizGameDTO();

        List<ScoreDTO> scoreDTOs = game.getScores()
                .entrySet()
                .stream()
                .map(e -> new ScoreDTO(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
        dto.setScores(scoreDTOs);

        dto.setRoomId(game.getRoom().getRoomId());
        dto.setCurrentQuestion(QuestionDTO.fromEntity(game.getCurrentQuestion()));
        dto.setQuestions(game.getQuestions().stream().filter(Objects::nonNull)
                .map(QuestionDTO::fromEntity).toList());
        dto.setScores(scoreDTOs);
        dto.setFinished(game.isFinished());
        dto.setGameId(game.getGameId());
        dto.setParticipants(game.getParticipants());
        dto.setCurrentQuestionIndex(game.getCurrentQuestionIndex());
        return dto;
    }


    /**
     * Récupère un jeu actif par son identifiant de salle
     * @param gameId identifiant de la salle
     * @return le jeu correspondant ou null s'il n'existe pas
     */
    @Transactional(readOnly = true)
    public QuizGame getGame(String gameId) {
        return gameRepository.findByGameId(gameId).orElseThrow(() ->
                new NoSuchElementException("Partie introuvable : " + gameId)
        );
    }


    @Transactional
    public QuizGame addParticipant(String gameId, String user) {
        QuizGame game = gameRepository
                .findByGameId((gameId)).orElseThrow(() ->
                        new NoSuchElementException("Partie introuvable : " + gameId)
                );
        game.addParticipant(user);
        return gameRepository.save(game);
    }

    @Transactional
    public QuizGame removeParticipant(String gameId, String user) {
        QuizGame game = gameRepository
                .findByGameId(gameId)
                .orElseThrow(() ->
                        new NoSuchElementException("Partie introuvable : " + gameId)
                );
            game.getParticipants().remove(user);
            return gameRepository.save(game);

    }


    @Transactional
    public QuizGameDTO getQuizGameDTO(String gameId, StartGameRequest payload) {
        Room room = roomService.getRoom(payload.getRoomId()).orElseThrow(() -> new IllegalArgumentException("Room not found: " + payload.getRoomId()));
        QuizGame game = createGame(gameId, room);
        addParticipant(gameId, payload.getUser());
        room.setQuizGame(game);
        roomService.saveRoom(room);

        String destination = "/game/" + game.getGameId();
        System.out.println("Sending game"+ game.getGameId() +" to " + destination);
        return (toDTO(game));
    }

    @Transactional
    public Optional<QuizGameDTO> removeParticipantFromGame(String gameId, String user) {
        QuizGame updated = removeParticipant(gameId, user);
        if (updated.getParticipants().isEmpty()) {
            gameRepository.deleteByGameId(gameId);
        }
        else {
            return Optional.ofNullable(toDTO(updated));
        }
        return Optional.empty();
    }
    @Transactional
    public QuizGameDTO enterQuizGame(String gameId, String user) {
        QuizGame updated = addParticipant(gameId, user);
        return toDTO(updated);
    }

}
