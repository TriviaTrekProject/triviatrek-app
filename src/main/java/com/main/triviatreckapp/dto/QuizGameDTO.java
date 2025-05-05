package com.main.triviatreckapp.dto;

import com.main.triviatreckapp.entities.Question;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class QuizGameDTO {
    private String roomId;
    private String gameId;
    private QuestionDTO currentQuestion;
    private List<QuestionDTO> questions;
    private Map<String, Integer> scores;
    private boolean finished;
    private List<String> participants;
    private int currentQuestionIndex;

}

