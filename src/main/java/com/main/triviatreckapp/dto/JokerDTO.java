package com.main.triviatreckapp.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class JokerDTO {
    private String username;
    private String idParticipant;
    private JokerType jokerType;
    
    public enum JokerType {
        PRIORITE_REPONSE,
        ANNULER_JOUEUR
    }
}