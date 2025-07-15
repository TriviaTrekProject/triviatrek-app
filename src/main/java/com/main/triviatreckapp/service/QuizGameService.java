package com.main.triviatreckapp.service;

import com.main.triviatreckapp.Request.PlayerJokerRequest;
import com.main.triviatreckapp.Request.StartGameRequest;
import com.main.triviatreckapp.dto.JokerDTO;
import com.main.triviatreckapp.dto.ParticipantDTO;
import com.main.triviatreckapp.dto.PlayerAnswerDTO;
import com.main.triviatreckapp.dto.QuestionDTO;
import com.main.triviatreckapp.dto.QuizGameDTO;
import com.main.triviatreckapp.dto.ScoreDTO;
import com.main.triviatreckapp.entities.Message;
import com.main.triviatreckapp.entities.Participant;
import com.main.triviatreckapp.entities.Question;
import com.main.triviatreckapp.entities.QuizGame;
import com.main.triviatreckapp.entities.Room;
import com.main.triviatreckapp.repository.QuestionRepository;
import com.main.triviatreckapp.repository.QuizGameRepository;
import com.main.triviatreckapp.repository.RoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Service
public class QuizGameService {
    private final QuestionRepository questionRepository;
    private final QuizGameRepository gameRepository;
    private final RoomService roomService;
    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;
    private final RoomRepository roomRepository;
    private final TaskScheduler gameTaskScheduler;

    // Pour suivre l'ordre des bonnes réponses par partie
    private final Map<String, List<String>> correctAnswerOrder = new ConcurrentHashMap<>();

    // Indique si la fenêtre de 30s est ouverte pour chaque gameId
    private final Map<String, Boolean> answerWindowStarted = new ConcurrentHashMap<>();

    // Pour pouvoir annuler le timer si besoin
    private final Map<String, ScheduledFuture<?>> scheduledFutures = new ConcurrentHashMap<>();

    // ApplicationContext pour récupérer le proxy Spring
    @Autowired
    private ApplicationContext applicationContext;


    @Value("${quiz.questions-per-game:10}")
    private int questionsPerGame;

    @Value("${quiz.correct-answer-points:1}")
    private int correctAnswerPoints;

    // Remplacez votre Map<String, Boolean> processingAnswers par :
    private final Map<String, ReentrantLock> gameLocks = new ConcurrentHashMap<>();
    // ❶ Suivi des joueurs déjà passés sur la question courante
    private final Map<String, Set<String>> answeredPlayers = new ConcurrentHashMap<>();


    public QuizGameService(QuestionRepository questionRepository,
                           QuizGameRepository gameRepository, RoomService roomService, ChatService chatService, SimpMessagingTemplate messagingTemplate,
                           RoomRepository roomRepository, TaskScheduler gameTaskScheduler)
     {
        this.questionRepository = questionRepository;
        this.gameRepository = gameRepository;
        this.roomService = roomService;
        this.chatService = chatService;
        this.messagingTemplate = messagingTemplate;
        this.roomRepository = roomRepository;
        this.gameTaskScheduler = gameTaskScheduler;
    }

    private String generateUniqueName(Collection<String> existing, String base) {
        String candidate = base;
        int suffix = 2;
        while (existing.contains(candidate)) {
            candidate = base + "(" + suffix + ")";
            suffix++;
        }
        return candidate;
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
        // Tente d'acquérir le verrou; si déjà en traitement, ignorer la nouvelle réponse.
        // récupère (ou crée) un lock "fair" pour cette partie
        ReentrantLock lock = gameLocks
                .computeIfAbsent(gameId, id -> new ReentrantLock(true)); // true = FIFO fairness

        lock.lock();
        try {

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
            // Récupérer le participant
            Participant part = game.getParticipants().stream()
                    .filter(p -> p.getId().toString().equals(playerAnswer.getParticipantId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Participant introuvable"));

        String username = part.getUsername();
        int basePoints = switch (current.getDifficulty()) {
                case "easy" -> 1;
                case "medium" -> 2;
                case "hard" -> 3;
                default -> throw new IllegalArgumentException("Difficulté inconnue: " + current.getDifficulty());
        };

            boolean isCorrect = playerAnswer.getAnswer().equals(current.getCorrectAnswer());
            // Liste des joueurs déjà corrects
            List<String> order = correctAnswerOrder
                    .computeIfAbsent(gameId, id -> new CopyOnWriteArrayList<>());


            if (isCorrect) {
                if (order.isEmpty()) {
                    // 1ʳᵉ bonne réponse : plein de points + démarrage du timer
                    order.add(username);
                    game.addScore(username, basePoints);
                    answerWindowStarted.put(gameId, true);

                    // Timer de 10s pour passer à la question suivante
                    ScheduledFuture<?> f = gameTaskScheduler.schedule(
                            () -> {
                                applicationContext.getBean(QuizGameService.class)
                                        .triggerNextQuestion(gameId);
                                },
                            Date.from(Instant.now().plusSeconds(10))
                    );
                    scheduledFutures.put(gameId, f);

                } else if (answerWindowStarted.getOrDefault(gameId, false)
                        && !order.contains(username)) {
                    // Bonnes réponses suivantes : barème dégressif
                    order.add(username);
                    int position = order.size(); // 2=>deuxième, 3=>troisième, etc.
                    int points = computeDecreasingPoints(basePoints, position);
                    game.addScore(username, points);
                }
                game.setWaitingForNext(true);

            }
            // ❸ Enregistrer que ce participant a répondu
            Set<String> answered = answeredPlayers
                    .computeIfAbsent(gameId, k -> ConcurrentHashMap.newKeySet());
            answered.add(playerAnswer.getParticipantId());


            // Sauvegarde et broadcast immédiat des scores mis à jour
            QuizGame saved = gameRepository.save(game);
            messagingTemplate.convertAndSend("/game/" + gameId, toDTO(saved));


            // Si tous ont répondu => annuler timer et passer à la q suivante
            int totalParticipants = game.getParticipants().size();
            if (answered.size() >= totalParticipants) {
                // annulation du timer programmé
                ScheduledFuture<?> future = scheduledFutures.remove(gameId);
                if (future != null) {
                    future.cancel(false);
                }

                // Timer de 10s pour passer à la question suivante
                ScheduledFuture<?> f = gameTaskScheduler.schedule(
                        () -> {
                            applicationContext.getBean(QuizGameService.class)
                                    .triggerNextQuestion(gameId);
                            // on vide le suivi des réponses
                            answeredPlayers.remove(gameId);

                        },
                        Date.from(Instant.now().plusSeconds(10))
                );
                scheduledFutures.put(gameId, f);

            }

            return toDTO(saved);

        } finally {
            lock.unlock();
            //  cleanup si la partie est finie
            if (gameRepository.findByGameId(gameId).map(QuizGame::isFinished).orElse(false)) {
                gameLocks.remove(gameId);
            }
        }

    }

    /**
     * Déclenché au bout de 30s après la 1ʳᵉ bonne réponse :
     * passe à la question suivante et broadcast.
     */
    @Transactional
    public void triggerNextQuestion(String gameId) {
        Optional<QuizGame> opt = gameRepository.findWithAllByGameId(gameId);
        if (opt.isEmpty()) return;

        QuizGame game = opt.get();
        if (!game.isFinished()) {
            // Réinitialiser l'état de la fenêtre
            resetWindow(gameId);
            game.setWaitingForNext(false);

            game.nextQuestion();

            QuizGame saved = gameRepository.save(game);
            messagingTemplate.convertAndSend("/game/" + gameId, toDTO(saved));

        }
    }

    private void resetWindow(String gameId) {
        answerWindowStarted.remove(gameId);
        correctAnswerOrder.remove(gameId);
        ScheduledFuture<?> f = scheduledFutures.remove(gameId);
        if (f != null) f.cancel(false);
    }

    /**
     * barème dégressif :
     * on retire 20% de la valeur de base par position-1
     */
    private int computeDecreasingPoints(int base, int position) {
        int decrement = base / 20;
        return Math.max(0, base - (position - 1) * decrement);
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

        // Convert Participant entities to ParticipantDTO objects
        List<ParticipantDTO> participantDTOs = game.getParticipants().stream()
                .map(participant -> new ParticipantDTO(participant.getId(), participant.getUsername(), participant.getDelaiReponse(), null))
                .toList();
        dto.setParticipants(participantDTOs);

        dto.setCurrentQuestionIndex(game.getCurrentQuestionIndex());
        dto.setWaitingForNext(game.isWaitingForNext());
        return dto;
    }

    // Helper method to find a participant by username
    private Participant findParticipantByUsername(List<Participant> participants, String username) {
        return participants.stream()
                .filter(p -> p.getUsername().equals(username))
                .findFirst()
                .orElse(null);
    }

    // Helper method to check if a participant with the given username exists
    private boolean hasParticipantWithUsername(List<Participant> participants, String username) {
        return participants.stream()
                .anyMatch(p -> p.getUsername().equals(username));
    }

    // Helper method to remove a participant by username
    private boolean removeParticipantByUsername(List<Participant> participants, String username) {
        return participants.removeIf(p -> p.getUsername().equals(username));
    }


    /**
     * Récupère un jeu actif par son identifiant de salle
     * @param gameId identifiant de la salle
     * @return le jeu correspondant ou null s'il n'existe pas
     */
    @Transactional(readOnly = true)
    public Optional<QuizGame> getGame(String gameId) {
        return gameRepository.findByGameId(gameId);
    }


    @Transactional
    public QuizGame addParticipant(String gameId, Long participantId) {
        QuizGame game = gameRepository
                .findByGameId((gameId)).orElseThrow(() ->
                        new NoSuchElementException("Partie introuvable : " + gameId)
                );
        game.addParticipant(
                game.getRoom().getParticipants().stream()
                        .filter(p -> p.getId().equals(participantId))
                        .findAny()
                        .orElseThrow(() -> new NoSuchElementException("Participant introuvable : " + participantId))
        );
        return gameRepository.save(game);
    }

    @Transactional
    public QuizGame removeParticipant(String gameId, Long participantId) {
        QuizGame game = gameRepository
                .findByGameId(gameId)
                .orElseThrow(() ->
                        new NoSuchElementException("Partie introuvable : " + gameId)
                );
        game.getParticipants().removeIf(p -> p.getId().equals(participantId));
            return gameRepository.save(game);

    }


    @Transactional
    public QuizGameDTO startQuizGameDTO(String gameId, StartGameRequest payload) {
        Room room = roomService.getRoom(payload.getRoomId())
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + payload.getRoomId()));
        QuizGame game = createGame(gameId, room);

        List<Participant> snapshot = new ArrayList<>(room.getParticipants());
        for (Participant participant : snapshot) {
            addParticipant(gameId, participant.getId());
        }

        room.setQuizGame(game);
        room.setActiveGame(true);
        roomService.saveRoom(room);

        return toDTO(game);
    }


    @Transactional
    public Optional<QuizGameDTO> removeParticipantFromGame(String gameId, Long participantId) {
        QuizGame updated = removeParticipant(gameId, participantId);
        if (updated.getParticipants().isEmpty()) {
            gameRepository.deleteByGameId(gameId);
            updated.getRoom().setActiveGame(false);
            roomService.saveRoom(updated.getRoom());
        }
        else {
            return Optional.ofNullable(toDTO(updated));
        }
        return Optional.empty();
    }
    @Transactional
    public QuizGameDTO enterQuizGame(String gameId, Long participantId) {
        QuizGame game = gameRepository.findByGameId(gameId)
                .orElseThrow(() -> new IllegalArgumentException("Jeu introuvable : " + gameId));


        if (game.getParticipants().stream().noneMatch(p -> p.getId().equals(participantId))) {
            QuizGame updated = addParticipant(gameId, participantId);
            return toDTO(updated);
        }

        return null;
    }

    @Transactional
    public QuizGameDTO getQuizGameDTO(String gameId) {
            return toDTO(getGame(gameId).orElseThrow(() -> new IllegalArgumentException("Game not found: " + gameId)));
    }

    /**
     * Process a joker used by a player
     * @param gameId the game ID
     * @param jokerRequest the joker request containing the joker type and participant ID
     */
    @Transactional
    public void processJoker(String gameId, PlayerJokerRequest jokerRequest) {
        if(jokerRequest.getJokerType() == PlayerJokerRequest.JokerType.PRIORITE_REPONSE) {

            // Create a Joker object
            JokerDTO joker = new JokerDTO(
                jokerRequest.getUsername(),
                jokerRequest.getParticipantId(),
                JokerDTO.JokerType.valueOf(jokerRequest.getJokerType().name())
            );

            // Send the Joker object to all subscribers of /game/{gameId}/joker/
            String destination = "/game/joker/"+gameId;
            messagingTemplate.convertAndSend(destination, joker);
        }
    }
}
