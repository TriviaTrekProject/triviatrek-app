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
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "roomId", nullable = false, unique = true)
    private String roomId;

    // → Passage de List<String> à List<Participant>
    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinTable(name = "room_participants",
            joinColumns = @JoinColumn(name = "room_id"),
            inverseJoinColumns = @JoinColumn(name = "participant_id"))
    private List<Participant> participants = new ArrayList<>();

    @OneToMany(fetch = FetchType.EAGER, mappedBy = "room",
            cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Message> messages = new ArrayList<>();

    @OneToOne(fetch = FetchType.EAGER, mappedBy = "room",
            cascade = CascadeType.ALL, orphanRemoval = true)
    private QuizGame quizGame;

    @Column(name = "activeGame")
    private boolean activeGame = false;

    public void addParticipant(Participant participant) {
        this.participants.add(participant);
    }

    public void removeParticipant(Participant participant) {
        this.participants.remove(participant);
    }

    public void setQuizGame(QuizGame game) {
        this.quizGame = game;
        if (game != null && game.getRoom() != this) {
            game.setRoom(this);
        }
    }
}
