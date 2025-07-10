package com.main.triviatreckapp.Request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PlayerJokerRequest {
    private JokerType jokerType;
    private String participantId;
    private String username;
    
    public enum JokerType {
        PRIORITE_REPONSE,
        ANNULER_JOUEUR
    }
}