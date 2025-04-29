package com.main.triviatreckapp.model;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class GameMessage {
    @lombok.Getter
    private String questionId;
    private String username;
    private String answer;

    public GameMessage() {
    }

    public GameMessage(String questionId, String username, String answer) {
        this.questionId = questionId;
        this.username = username;
        this.answer = answer;
    }

}