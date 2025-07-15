package com.main.triviatreckapp.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "quiz_games")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class QuizGame {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "gameId", nullable = false, unique = true)
    private String gameId;              // Identifiant métier unique

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", unique = true)
    private Room room;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "question_quiz_game",
            joinColumns = @JoinColumn(name = "quiz_game_id"),
            inverseJoinColumns = @JoinColumn(name = "question_id")
    )
    @OrderColumn(name = "question_order")        // <-- ajouté pour indexer la liste
    private List<Question> questions = new ArrayList<>();

    // score par joueur (clé = nom ou id utilisateur)
    @ElementCollection
    @CollectionTable(name = "quiz_game_scores", joinColumns = @JoinColumn(name = "quiz_game_id"))
    @MapKeyColumn(name = "player")
    @Column(name = "score")
    private Map<String, Integer> scores = new HashMap<>();
    private boolean finished = false;           // Indique si le jeu est terminé
    @Column(name = "waiting_for_next", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean waitingForNext = false;
    @ManyToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinTable(
            name = "quiz_game_participants",
            joinColumns = @JoinColumn(name = "quiz_game_id"),
            inverseJoinColumns = @JoinColumn(name = "participant_id")
    )
    private List<Participant> participants = new ArrayList<>();


    // index dans la liste questions
    private int currentQuestionIndex = 0;

    public Question getCurrentQuestion() {
        if (currentQuestionIndex < questions.size()) {
            return questions.get(currentQuestionIndex);
        }
        return null;
    }

    // Méthode pour passer à la question suivante
    public boolean nextQuestion() {
        currentQuestionIndex++;
        if (currentQuestionIndex >= questions.size()) {
            finished = true;
            return false;
        }
        return true;
    }

    // Ajouter un score à un joueur
    public void addScore(String player, int points) {
        int newScore = scores.getOrDefault(player, 0) + points;
        newScore = Math.max(newScore, 0);
        scores.put(player, newScore);
    }

    public void setRoom(Room room) {
        this.room = room;
        if (room != null && room.getQuizGame() != this) {
            room.setQuizGame(this);
        }
    }

    public void addParticipant(Participant participant) {
        participants.add(participant);
    }

    public void addQuestion(Question q) {
        this.questions.add(q);
    }


}
