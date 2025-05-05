package com.main.triviatreckapp.repository;

import com.main.triviatreckapp.entities.QuizGame;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface QuizGameRepository extends JpaRepository<QuizGame, Long> {

    /**
     * Récupère une partie par l'identifiant métier de la salle associée.
     */
    Optional<QuizGame> findByRoomRoomId(String roomId);

    Optional<QuizGame> findByGameId(String gameId);
}

