package com.main.triviatreckapp.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class QuizGameDTO {
    private String roomId;
    private String gameId;
    private QuestionDTO currentQuestion;
    private List<QuestionDTO> questions;
    private List<ScoreDTO> scores;
    private boolean finished;
    private List<ParticipantDTO> participants;
    private int currentQuestionIndex;
    private boolean waitingForNext;

}
