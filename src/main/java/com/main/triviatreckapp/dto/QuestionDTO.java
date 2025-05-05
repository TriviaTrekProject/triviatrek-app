package com.main.triviatreckapp.dto;

import com.main.triviatreckapp.entities.Question;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestionDTO {
    private Long id;
    private String question;
    private String difficulty;
    private String category;
    private String correctAnswer;
    private List<String> incorrectAnswers;
    private List<String> options;
    private int correctIndex;

    public static QuestionDTO fromEntity(Question q) {
        return new QuestionDTO(
                q.getId(),
                q.getQuestion(),
                q.getDifficulty(),
                q.getCategory(),
                q.getCorrectAnswer(),
                q.getIncorrectAnswers(),
                q.getOptions(),
                q.getCorrectIndex()
        );
    }

}



