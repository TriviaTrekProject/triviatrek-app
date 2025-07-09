package com.main.triviatreckapp.service;

import com.main.triviatreckapp.entities.Question;
import com.main.triviatreckapp.entities.QuizGame;
import com.main.triviatreckapp.repository.QuestionRepository;
import com.main.triviatreckapp.repository.QuizGameRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final QuizGameRepository quizGameRepository;

    public QuestionService(QuestionRepository questionRepository, QuizGameRepository quizGameRepository) {
        this.questionRepository = questionRepository;
        this.quizGameRepository = quizGameRepository;
    }

    public Iterable<Question> list() {
        return questionRepository.findAll();
    }

    public Iterable<Question> saveAll(List<Question> questions) {
        return questionRepository.saveAll(questions);
    }

    @Transactional
    public void deleteAll() {
        // First, get all quiz games
        List<QuizGame> quizGames = quizGameRepository.findAll();

        // For each quiz game, clear the questions collection
        for (QuizGame quizGame : quizGames) {
            quizGame.getQuestions().clear();
            quizGameRepository.save(quizGame);
        }

        // Now it's safe to delete all questions
        questionRepository.deleteAll();
    }
}
