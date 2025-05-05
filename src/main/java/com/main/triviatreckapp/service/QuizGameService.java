package com.main.triviatreckapp.service;

import com.main.triviatreckapp.dto.PlayerAnswerDTO;
import com.main.triviatreckapp.dto.QuizGameDTO;
import com.main.triviatreckapp.entities.Question;
import com.main.triviatreckapp.entities.QuizGame;
import com.main.triviatreckapp.entities.Room;
import com.main.triviatreckapp.repository.QuestionRepository;
import com.main.triviatreckapp.repository.QuizGameRepository;
import com.main.triviatreckapp.repository.RoomRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
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
     * @param roomId identifiant de la salle
     * @return l'objet QuizGame créé
     */
    public QuizGame createOrRestartGame(String roomId) {
        // Sélectionner des questions aléatoires depuis la base de données
        List<Question> randomQuestions = getRandomQuestions(questionsPerGame);
        Room room = roomRepository.findByRoomId(roomId).orElseThrow(() -> new IllegalArgumentException("Salle introuvable : " + roomId));
        // Créer un nouveau jeu
        QuizGame game = new QuizGame();
        game.setRoom(room);
        game.setQuestions(randomQuestions);
        game.setCurrentQuestionIndex(0);
        game.setScores(new HashMap<>());
        game.setFinished(false);


        return gameRepository.save(game);
    }

    /**
     * Traite la réponse d'un joueur à la question courante
     * @param roomId identifiant de la salle
     * @param playerAnswer DTO contenant l'identifiant du joueur et sa réponse
     * @return le jeu mis à jour
     */
    public QuizGame processAnswer(String roomId, PlayerAnswerDTO playerAnswer) {
        Optional<QuizGame> opt = gameRepository.findByRoomRoomId(roomId);
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

        if (playerAnswer.getAnswerIndex() == current.getCorrectIndex()) {
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
        dto.setRoomId(game.getRoom().getRoomId());
        dto.setCurrentQuestion(game.getCurrentQuestion());
        dto.setScores(game.getScores());
        dto.setFinished(game.isFinished());
        return dto;
    }


    /**
     * Récupère un jeu actif par son identifiant de salle
     * @param roomId identifiant de la salle
     * @return le jeu correspondant ou null s'il n'existe pas
     */
    public QuizGame getGame(String roomId) {
        return gameRepository.findByRoomRoomId(roomId).orElse(null);
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
        return all.subList(0, count);
    }

}
