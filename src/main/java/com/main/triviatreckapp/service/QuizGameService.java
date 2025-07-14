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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class QuizGameService {
    private final QuestionRepository questionRepository;
    private final QuizGameRepository gameRepository;
    private final RoomService roomService;
    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;
    private final RoomRepository roomRepository;


    @Value("${quiz.questions-per-game:10}")
    private int questionsPerGame;

    @Value("${quiz.correct-answer-points:1}")
    private int correctAnswerPoints;

    // Carte de verrouillage pour éviter le traitement concurrent de réponses
    private final ConcurrentHashMap<String, Boolean> processingAnswers = new ConcurrentHashMap<>();


    public QuizGameService(QuestionRepository questionRepository,
                           QuizGameRepository gameRepository, RoomService roomService, ChatService chatService, SimpMessagingTemplate messagingTemplate,
                           RoomRepository roomRepository) {
        this.questionRepository = questionRepository;
        this.gameRepository = gameRepository;
        this.roomService = roomService;
        this.chatService = chatService;
        this.messagingTemplate = messagingTemplate;
        this.roomRepository = roomRepository;
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
        if (processingAnswers.putIfAbsent(gameId, Boolean.TRUE) != null) {
            Optional<QuizGame> optExisting = gameRepository.findByGameId(gameId);
            return optExisting.map(this::toDTO).orElse(null);
        }

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
        Participant part = game.getParticipants().stream().filter(participant -> participant.getId().toString().equals(playerAnswer.getParticipantId())).findFirst().orElseThrow(() -> new IllegalArgumentException("Participant introuvable : " + playerAnswer.getParticipantId()));
        String messageSystem = part.getUsername();
        int mutiplicater = switch (current.getDifficulty()) {
                case "easy" -> 1;
                case "medium" -> 2;
                case "hard" -> 3;
                default -> throw new IllegalArgumentException("Difficulté inconnue: " + current.getDifficulty());
        };



        if (Objects.equals(playerAnswer.getAnswer(), current.getCorrectAnswer())) {
            game.addScore(part.getUsername(), mutiplicater * correctAnswerPoints);
            messageSystem = messageSystem.concat(" a répondu correctement !");
        }
        else {
            game.addScore(part.getUsername(), -2 * correctAnswerPoints * mutiplicater);
            messageSystem = messageSystem.concat(" a répondu faux !");
        }

            String destination = "/chatroom/" + game.getRoom().getRoomId();
            Message message = chatService.saveMessage(game.getRoom().getRoomId(), "GAME_SYSTEM_"+game.getCurrentQuestionIndex(), messageSystem);
            game.getRoom().getMessages().add(message);
            roomRepository.save(game.getRoom());
            messagingTemplate.convertAndSend(destination, Optional.ofNullable(roomService.convertRoomToDTO(roomRepository.save(game.getRoom()))));

            // Reset delaiReponse for all participants
            for (Participant participant : game.getParticipants()) {
                participant.setDelaiReponse(0);
            }

            game.nextQuestion();

            return toDTO(gameRepository.save(game));

        } finally {
            processingAnswers.remove(gameId);
        }

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
