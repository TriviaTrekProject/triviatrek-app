package com.main.triviatreckapp.service;

import com.main.triviatreckapp.dto.PlayerAnswerDTO;
import com.main.triviatreckapp.dto.QuizGameDTO;
import com.main.triviatreckapp.entities.Question;
import com.main.triviatreckapp.entities.QuizGame;
import com.main.triviatreckapp.entities.Room;
import com.main.triviatreckapp.repository.QuestionRepository;
import com.main.triviatreckapp.repository.RoomRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class QuizGameService {
    private final QuestionRepository questionRepository;
    private final RoomRepository roomRepository;
    private final Map<String, QuizGame> activeGames = new ConcurrentHashMap<>();

    @Value("${quiz.questions-per-game:10}")
    private int questionsPerGame;

    @Value("${quiz.correct-answer-points:1}")
    private int correctAnswerPoints;

    public QuizGameService(QuestionRepository questionRepository, RoomRepository roomRepository) {
        this.questionRepository = questionRepository;
        this.roomRepository = roomRepository;
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

        // Stocker le jeu dans la map des jeux actifs
        activeGames.put(roomId, game);

        return game;
    }

    /**
     * Traite la réponse d'un joueur à la question courante
     * @param roomId identifiant de la salle
     * @param playerAnswer DTO contenant l'identifiant du joueur et sa réponse
     * @return le jeu mis à jour
     */
    public QuizGame processAnswer(String roomId, PlayerAnswerDTO playerAnswer) {
        QuizGame game = activeGames.get(roomId);
        if (game == null || game.isFinished()) {
            return null;
        }

        Question currentQuestion = game.getCurrentQuestion();
        if (currentQuestion == null) {
            game.setFinished(true);
            return game;
        }

        // Vérifier si la réponse est correcte
        if (playerAnswer.getAnswerIndex() == currentQuestion.getCorrectIndex()) {
            // Si correcte, ajouter des points au joueur
            game.addScore(playerAnswer.getPlayer(), correctAnswerPoints);
        }

        // Passer à la question suivante (et vérifier si c'est la fin du jeu)
        game.nextQuestion();

        return game;
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
        return activeGames.get(roomId);
    }

    /**
     * Sélectionne aléatoirement un nombre spécifié de questions depuis la base de données
     * @param count nombre de questions à sélectionner
     * @return liste des questions sélectionnées
     */
    private List<Question> getRandomQuestions(int count) {
        // Cette méthode pourrait être optimisée selon votre implémentation de repository
        List<Question> allQuestions = questionRepository.findAll();

        if (allQuestions.size() <= count) {
            return allQuestions;
        }

        // Mélange la liste de questions
        Collections.shuffle(allQuestions);

        // Prend les 'count' premières questions
        return allQuestions.subList(0, count);
    }
}
