package com.main.triviatreckapp.dto;

import com.main.triviatreckapp.entities.Question;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Collections;
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

    public static QuestionDTO fromEntity(Question q) {
        return new QuestionDTO(
                q.getId(),
                q.getQuestion(),
                q.getDifficulty(),
                q.getCategory(),
                q.getCorrectAnswer(),
                q.getIncorrectAnswers(),
                generateOptions(q)
        );
    }

    public static List<String> generateOptions(Question q) {
        List<String> options = new ArrayList<>();
        if (q.getCorrectAnswer() != null) {
            options.add(q.getCorrectAnswer());
        }
        if (q.getIncorrectAnswers() != null) {
            options.addAll(q.getIncorrectAnswers());
        }
        // Mélange aléatoire des options pour éviter que la bonne réponse soit toujours à la même place
        Collections.shuffle(options);
        return options;
    }
}




