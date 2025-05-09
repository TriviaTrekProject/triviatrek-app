package com.main.triviatreckapp.service;

import com.main.triviatreckapp.entities.Question;
import com.main.triviatreckapp.repository.QuestionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class QuestionService {

    private final QuestionRepository questionRepository;

    public QuestionService(QuestionRepository  questionRepository) {
        this.questionRepository = questionRepository;
    }

    public Iterable<Question> list() {
        return questionRepository.findAll();
    }

    public Iterable<Question> saveAll(List<Question> questions) {
        return questionRepository.saveAll(questions);
    }
}
