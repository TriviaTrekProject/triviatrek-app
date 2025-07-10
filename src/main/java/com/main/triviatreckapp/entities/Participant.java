package com.main.triviatreckapp.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "participants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Participant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "delai_reponse")
    private int delaiReponse;

    // Constructeur avec génération automatique de participantId
    public Participant(String username, int delaiReponse) {
        this.username = username;
        this.delaiReponse = delaiReponse;
    }
}

