package com.main.triviatreckapp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlayerAnswerDTO {
    private String participantId;     // Identifiant du joueur
    private String answer;   // Index de la réponse choisie (0-based)
}
