package com.main.triviatreckapp.service;

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
@Transactional
public class QuizGameService {
    private final QuestionRepository questionRepository;
    private final RoomRepository roomRepository;
    private final QuizGameRepository gameRepository;

    @Value("${quiz.questions-per-game:10}")
    private int questionsPerGame;

    @Value("${quiz.correct-answer-points:1}")
    private int correctAnswerPoints;

    public QuizGameService(QuestionRepository questionRepository,
                           RoomRepository roomRepository,
                           QuizGameRepository gameRepository) {
        this.questionRepository = questionRepository;
        this.roomRepository = roomRepository;
        this.gameRepository = gameRepository;
    }

    /**
     * Crée ou redémarre une partie dans une salle
     * @param gameId identifiant de la salle
     * @return l'objet QuizGame créé
     */
    public QuizGame createGame(String gameId, Room room) {
        // Sélectionner des questions aléatoires depuis la base de données
        List<Question> randomQuestions = getRandomQuestions(questionsPerGame);
                // Créer un nouveau jeu
            QuizGame game = new QuizGame();
            game.setRoom(room);
            game.setGameId(gameId);
            game.setQuestions(questionRepository.findRandomQuestions(10));
            game.setCurrentQuestionIndex(0);
            game.setScores(new HashMap<>());
            game.setFinished(false);

        return gameRepository.save(game);
    }

    /**
     * Traite la réponse d'un joueur à la question courante
     * @param gameId identifiant de la salle
     * @param playerAnswer DTO contenant l'identifiant du joueur et sa réponse
     * @return le jeu mis à jour
     */
    public QuizGame processAnswer(String gameId, PlayerAnswerDTO playerAnswer) {
        Optional<QuizGame> opt = gameRepository.findByGameId(gameId);
        if (opt.isEmpty()) {
            return null;
        }

        QuizGame game = opt.get();
        if (game.isFinished()) {
            return game;
        }

        Question current = game.getCurrentQuestion();
        if (current == null) {
            game.setFinished(true);
            return gameRepository.save(game);
        }
        System.out.println("Player " + playerAnswer.getPlayer() + " answered " + playerAnswer.getAnswerIndex() + " for question " + current.getId());
        System.out.println("Correct answer is " + current.getCorrectIndex());


        if (playerAnswer.getAnswerIndex()  == current.getCorrectIndex()) {
            game.addScore(playerAnswer.getPlayer(), correctAnswerPoints);
        }

        game.nextQuestion();
        return gameRepository.save(game);
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
        dto.setQuestions(game.getQuestions().stream().map(QuestionDTO::fromEntity).toList());
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

    /**
     * Sélectionne aléatoirement un nombre spécifié de questions depuis la base de données
     * @param count nombre de questions à sélectionner
     * @return liste des questions sélectionnées
     */
    private List<Question> getRandomQuestions(int count) {

        List<Question> all = questionRepository.findAll();
        if (all.size() <= count) {
            return all;
        }
        Collections.shuffle(all);
        return all.subList(0, count).stream().map(q -> questionRepository.findById(q.getId())
                        .orElseThrow(() -> new NoSuchElementException("Question introuvable : " + q.getId())))
                .toList();

    }

    public QuizGame addParticipant(String gameId, String user) {
        QuizGame game = gameRepository
                .findByGameId((gameId)).orElseThrow(() ->
                        new NoSuchElementException("Partie introuvable : " + gameId)
                );
        game.addParticipant(user);
        return gameRepository.save(game);
    }


    public QuizGame removeParticipant(String gameId, String user) {
        QuizGame game = gameRepository
                .findByGameId(gameId)
                .orElseThrow(() ->
                        new NoSuchElementException("Partie introuvable : " + gameId)
                );
            game.getParticipants().remove(user);
            return gameRepository.save(game);

    }

}
