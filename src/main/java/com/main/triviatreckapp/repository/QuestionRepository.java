package com.main.triviatreckapp.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.main.triviatreckapp.entities.Question;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.util.*;

@Repository
public class QuestionRepository {

    private final ObjectMapper mapper;
    private List<Question> questions = Collections.emptyList();

    public QuestionRepository(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @PostConstruct
    public void loadQuestions() throws IOException {
        var resource = new ClassPathResource("static/Questions.json");
        this.questions = mapper.readValue(
                resource.getInputStream(),
                new TypeReference<List<Question>>() {}
        );
    }

    public List<Question> findAll() {
        return questions;
    }


    // Récupérer des questions par catégorie
    public List<Question> findByCategory(String category) {
        return questions.stream()
                .filter(q -> q.getCategory().equals(category))
                .toList();
    }

    // Récupérer des questions par niveau de difficulté
    public List<Question> findByDifficulty(String difficulty) {
        return questions.stream().filter(q -> q.getDifficulty().equals(difficulty)).toList();
    }
    
    // Récupérer des questions aléatoires (requête personnalisée)
    public List<Question> findRandomQuestions(int limit) {
        if (questions.size() <= limit) {
            return new ArrayList<>(questions);
        }
        List<Question> copy = new ArrayList<>(questions);
        Collections.shuffle(copy);
        return copy.subList(0, limit);
    }

    public Optional<Question> findById(Long id) {
        return questions.stream().filter(q -> Objects.equals(q.getId(), id))
                .findFirst();
    }
}
