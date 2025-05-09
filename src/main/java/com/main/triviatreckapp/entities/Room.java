package com.main.triviatreckapp.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "rooms")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                    // Identifiant auto-incrémenté pour PostgreSQL

    @Column(name = "roomId", nullable = false, unique = true)
    private String roomId;              // Identifiant métier unique

    @Column(name = "participant")
    private List<String> participants = new ArrayList<>();

    @OneToMany(fetch = FetchType.EAGER, mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Message> messages = new ArrayList<>();

    @OneToOne(fetch = FetchType.EAGER, mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    private QuizGame quizGame;

    public void addParticipant(String participant) {
        participants.add(participant);
    }

    public void setQuizGame(QuizGame game) {
        this.quizGame = game;
        if (game != null && game.getRoom() != this) {
            game.setRoom(this);
        }
    }

}