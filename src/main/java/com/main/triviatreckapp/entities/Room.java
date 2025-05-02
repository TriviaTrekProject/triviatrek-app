package com.main.triviatreckapp.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    @ElementCollection
    @CollectionTable(name = "room_participants",
            joinColumns = @JoinColumn(name = "room_id"))
    @Column(name = "participant")
    private Set<String> participants = new HashSet<>();

    @OneToMany(fetch = FetchType.EAGER, mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Message> messages = new HashSet<>();

}