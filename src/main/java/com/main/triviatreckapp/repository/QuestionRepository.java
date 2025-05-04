package com.main.triviatreckapp.repository;

import com.main.triviatreckapp.entities.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {
    
    // Récupérer des questions par catégorie
    List<Question> findByCategory(String category);
    
    // Récupérer des questions par niveau de difficulté
    List<Question> findByDifficulty(String difficulty);
    
    // Récupérer des questions aléatoires (requête personnalisée)
    @Query(value = "SELECT * FROM questions ORDER BY RANDOM() LIMIT :limit", nativeQuery = true)
    List<Question> findRandomQuestions(int limit);
}
