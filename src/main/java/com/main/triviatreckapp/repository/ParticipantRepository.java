package com.main.triviatreckapp.repository;

import com.main.triviatreckapp.entities.Participant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ParticipantRepository
        extends JpaRepository<Participant, Long> {
    Optional<Participant> findByUsername(String username);
}
