package com.main.triviatreckapp.repository;

import com.main.triviatreckapp.entities.QuizGame;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface QuizGameRepository extends JpaRepository<QuizGame, Long> {

    /**
     * Récupère une partie par l'identifiant métier de la salle associée.
     */
    Optional<QuizGame> findByRoomRoomId(String roomId);

    @EntityGraph(attributePaths = {"room"})
    Optional<QuizGame> findByGameId(String gameId);

    /**
     * Charge la partie avec ses questions, ses participants et ses scores.
     */
    @EntityGraph(attributePaths = {"questions", "participants", "scores"})
    @Query("""
        SELECT g
        FROM QuizGame g
        LEFT JOIN FETCH g.room r
        LEFT JOIN FETCH g.questions q
        LEFT JOIN FETCH g.participants p
        WHERE g.gameId = :gameId
        """)
    Optional<QuizGame> findWithAllByGameId(@Param("gameId") String gameId);


    void deleteByGameId(String gameId);
}

